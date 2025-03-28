/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include constants.cl
#include vanilla_uvs.cl

int priority_map(int p, int distance, int _min10, int avg1, int avg2, int avg3);
int count_prio_offset(__local struct shared_data *shared, int priority);
void get_face(
  __local struct shared_data *shared,
  __constant struct uniform *uni,
  __global const int4 *vb,
  uint localId, struct ModelInfo minfo,
  /* out */ int *prio, int *dis, int4 *o1, int4 *o2, int4 *o3);
void add_face_prio_distance(
  __local struct shared_data *shared,
  __constant struct uniform *uni,
  uint localId, struct ModelInfo minfo, int4 thisrvA, int4 thisrvB, int4 thisrvC, int thisPriority, int thisDistance, int4 pos);
int map_face_priority(__local struct shared_data *shared, uint localId, struct ModelInfo minfo, int thisPriority, int thisDistance, int *prio);
void insert_dfs(__local struct shared_data *shared, uint localId, struct ModelInfo minfo, int adjPrio, int distance, int prioIdx);
int tile_height(read_only image3d_t tileHeightMap, int z, int x, int y);
int4 hillskew_vertex(read_only image3d_t tileHeightMap, int4 v, int hillskew, int y, int plane);
void undoVanillaShading(int4 *vertex, float3 unrotatedNormal);
void sort_and_insert(
  __local struct shared_data *shared,
  __global const float4 *uv,
  __global const float4 *normal,
  __global int4 *vout,
  __global float4 *uvout,
  __global float4 *normalout,
  __constant struct uniform *uni,
  uint localId,
  struct ModelInfo minfo,
  int thisPriority,
  int thisDistance,
  int4 thisrvA,
  int4 thisrvB,
  int4 thisrvC,
  read_only image3d_t tileHeightMap
);

// Calculate adjusted priority for a face with a given priority, distance, and
// model global min10 and face distance averages. This allows positioning faces
// with priorities 10/11 into the correct 'slots' resulting in 18 possible
// adjusted priorities
int priority_map(int p, int distance, int _min10, int avg1, int avg2, int avg3) {
  // (10, 11)  0  1  2  (10, 11)  3  4  (10, 11)  5  6  7  8  9  (10, 11)
  //   0   1   2  3  4    5   6   7  8    9  10  11 12 13 14 15   16  17
  switch (p) {
    case 0: return 2;
    case 1: return 3;
    case 2: return 4;
    case 3: return 7;
    case 4: return 8;
    case 5: return 11;
    case 6: return 12;
    case 7: return 13;
    case 8: return 14;
    case 9: return 15;
    case 10:
      if (distance > avg1) {
        return 0;
      } else if (distance > avg2) {
        return 5;
      } else if (distance > avg3) {
        return 9;
      } else {
        return 16;
      }
    case 11:
      if (distance > avg1 && _min10 > avg1) {
        return 1;
      } else if (distance > avg2 && (_min10 > avg1 || _min10 > avg2)) {
        return 6;
      } else if (distance > avg3 && (_min10 > avg1 || _min10 > avg2 || _min10 > avg3)) {
        return 10;
      } else {
        return 17;
      }
    default:
      // this can't happen unless an invalid priority is sent. just assume 0.
      return 0;
  }
}

// calculate the number of faces with a lower adjusted priority than
// the given adjusted priority
int count_prio_offset(__local struct shared_data *shared, int priority) {
  // this shouldn't ever be outside of (0, 17) because it is the return value from priority_map
  priority = clamp(priority, 0, 17);
  int total = 0;
  for (int i = 0; i < priority; i++) {
    total += shared->totalMappedNum[i];
  }
  return total;
}

void get_face(
  __local struct shared_data *shared,
  __constant struct uniform *uni,
  __global const int4 *vb,
  uint localId, struct ModelInfo minfo,
  /* out */ int *prio, int *dis, int4 *o1, int4 *o2, int4 *o3
) {
  uint size = minfo.size;
  uint offset = minfo.offset;
  int flags = minfo.flags;
  uint ssboOffset;

  if (localId < size) {
    ssboOffset = localId;
  } else {
    ssboOffset = 0;
  }

  int4 thisA = vb[offset + ssboOffset * 3];
  int4 thisB = vb[offset + ssboOffset * 3 + 1];
  int4 thisC = vb[offset + ssboOffset * 3 + 2];

  if (localId < size) {
    int radius = (flags >> 12) & 0xfff;
    int orientation = flags & 0x7ff;

    // rotate for model orientation
    int4 thisrvA = rotate_ivec(uni, thisA, orientation);
    int4 thisrvB = rotate_ivec(uni, thisB, orientation);
    int4 thisrvC = rotate_ivec(uni, thisC, orientation);

    // calculate distance to face
    int thisPriority = (thisA.w >> 16) & 0xF;// all vertices on the face have the same priority
    int thisDistance;
    if (radius == 0) {
      thisDistance = 0;
    } else {
      thisDistance = face_distance(uni, thisrvA, thisrvB, thisrvC) + radius;
    }

    *o1 = thisrvA;
    *o2 = thisrvB;
    *o3 = thisrvC;

    *prio = thisPriority;
    *dis = thisDistance;
  } else {
    *o1 = (int4)(0, 0, 0, 0);
    *o2 = (int4)(0, 0, 0, 0);
    *o3 = (int4)(0, 0, 0, 0);
    *prio = 0;
    *dis = 0;
  }
}

void add_face_prio_distance(
  __local struct shared_data *shared,
  __constant struct uniform *uni,
  uint localId, struct ModelInfo minfo, int4 thisrvA, int4 thisrvB, int4 thisrvC, int thisPriority, int thisDistance, int4 pos) {
  uint size = minfo.size;
  if (localId < size) {
    // if the face is not culled, it is calculated into priority distance averages
    if (face_visible(uni, thisrvA, thisrvB, thisrvC, pos)) {
      atomic_add(&shared->totalNum[thisPriority], 1);
      atomic_add(&shared->totalDistance[thisPriority], thisDistance);

      // calculate minimum distance to any face of priority 10 for positioning the 11 faces later
      if (thisPriority == 10) {
        atomic_min(&shared->min10, thisDistance);
      }
    }
  }
}

int map_face_priority(__local struct shared_data *shared, uint localId, struct ModelInfo minfo, int thisPriority, int thisDistance, int *prio) {
  uint size = minfo.size;

  // Compute average distances for 0/2, 3/4, and 6/8

  if (localId < size) {
    int avg1 = 0;
    int avg2 = 0;
    int avg3 = 0;

    if (shared->totalNum[1] > 0 || shared->totalNum[2] > 0) {
      avg1 = (shared->totalDistance[1] + shared->totalDistance[2]) / (shared->totalNum[1] + shared->totalNum[2]);
    }

    if (shared->totalNum[3] > 0 || shared->totalNum[4] > 0) {
      avg2 = (shared->totalDistance[3] + shared->totalDistance[4]) / (shared->totalNum[3] + shared->totalNum[4]);
    }

    if (shared->totalNum[6] > 0 || shared->totalNum[8] > 0) {
      avg3 = (shared->totalDistance[6] + shared->totalDistance[8]) / (shared->totalNum[6] + shared->totalNum[8]);
    }

    int adjPrio = priority_map(thisPriority, thisDistance, shared->min10, avg1, avg2, avg3);
    int prioIdx = atomic_add(&shared->totalMappedNum[adjPrio], 1);

    *prio = adjPrio;
    return prioIdx;
  }

  *prio = 0;
  return 0;
}

void insert_dfs(__local struct shared_data *shared, uint localId, struct ModelInfo minfo, int adjPrio, int distance, int prioIdx) {
  uint size = minfo.size;

  if (localId < size) {
    // calculate base offset into dfs based on number of faces with a lower priority
    int baseOff = count_prio_offset(shared, adjPrio);
    // store into face array offset array by unique index
    shared->dfs[baseOff + prioIdx] = ((int) localId << 16) | distance;
  }
}

int tile_height(read_only image3d_t tileHeightMap, int z, int x, int y) {
  #define ESCENE_OFFSET 40 // (184-104)/2
  const sampler_t tileHeightSampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE;
  int4 coord = (int4)(x + ESCENE_OFFSET, y + ESCENE_OFFSET, z, 0);
  return read_imagei(tileHeightMap, tileHeightSampler, coord).x << 3;
}

int4 hillskew_vertex(read_only image3d_t tileHeightMap, int4 v, int hillskew, int y, int plane) {
  if (hillskew == 1) {
    int px = v.x & 127;
    int pz = v.z & 127;
    int sx = v.x >> 7;
    int sz = v.z >> 7;
    int h1 = (px * tile_height(tileHeightMap, plane, sx + 1, sz) + (128 - px) * tile_height(tileHeightMap, plane, sx, sz)) >> 7;
    int h2 = (px * tile_height(tileHeightMap, plane, sx + 1, sz + 1) + (128 - px) * tile_height(tileHeightMap, plane, sx, sz + 1)) >> 7;
    int h3 = (pz * h2 + (128 - pz) * h1) >> 7;
    return (int4)(v.x, v.y + h3 - y, v.z, v.w);
  } else {
    return v;
  }
}

void undoVanillaShading(int4 *vertex, float3 unrotatedNormal) {
    unrotatedNormal = normalize(unrotatedNormal);

    const float3 LIGHT_DIR_MODEL = (float3)(0.57735026, 0.57735026, 0.57735026);
    // subtracts the X lowest lightness levels from the formula.
    // helps keep darker colors appropriately dark
    const int IGNORE_LOW_LIGHTNESS = 3;
    // multiplier applied to vertex' lightness value.
    // results in greater lightening of lighter colors
    const float LIGHTNESS_MULTIPLIER = 3.f;
    // the minimum amount by which each color will be lightened
    const int BASE_LIGHTEN = 10;

    int hsl = vertex->w;
    int saturation = hsl >> 7 & 0x7;
    int lightness = hsl & 0x7F;
    float vanillaLightDotNormals = dot(LIGHT_DIR_MODEL, unrotatedNormal);
    if (vanillaLightDotNormals > 0) {
        float lighten = max(0, lightness - IGNORE_LOW_LIGHTNESS);
        lightness += (int) ((lighten * LIGHTNESS_MULTIPLIER + BASE_LIGHTEN - lightness) * vanillaLightDotNormals);
    }
    int maxLightness;
    #if LEGACY_GREY_COLORS
    maxLightness = 55;
    #else
    const int MAX_BRIGHTNESS_LOOKUP_TABLE[8] = { 127, 61, 59, 57, 56, 56, 55, 55 };
    maxLightness = MAX_BRIGHTNESS_LOOKUP_TABLE[saturation];
    #endif
    lightness = min(lightness, maxLightness);
    hsl &= ~0x7F;
    hsl |= lightness;
    vertex->w = hsl;
}

void sort_and_insert(
  __local struct shared_data *shared,
  __global const float4 *uv,
  __global const float4 *normal,
  __global int4 *vout,
  __global float4 *uvout,
  __global float4 *normalout,
  __constant struct uniform *uni,
  uint localId,
  struct ModelInfo minfo,
  int thisPriority,
  int thisDistance,
  int4 thisrvA,
  int4 thisrvB,
  int4 thisrvC,
  read_only image3d_t tileHeightMap
) {
  /* compute face distance */
  uint offset = minfo.offset;
  uint size = minfo.size;

  if (localId < size) {
    int outOffset = minfo.idx;
    int uvOffset = minfo.uvOffset;
    int flags = minfo.flags;
    int4 pos = (int4)(minfo.x, minfo.y, minfo.z, 0);
    int orientation = flags & 0x7ff;

    const int priorityOffset = count_prio_offset(shared, thisPriority);
    const int numOfPriority = shared->totalMappedNum[thisPriority];
    int start = priorityOffset; // index of first face with this priority
    int end = priorityOffset + numOfPriority; // index of last face with this priority
    int myOffset = priorityOffset;
    
    // we only have to order faces against others of the same priority
    // calculate position this face will be in
    for (int i = start; i < end; ++i) {
      int d1 = shared->dfs[i];
      uint theirId = d1 >> 16;
      int theirDistance = d1 & 0xffff;

      // the closest faces draw last, so have the highest index
      // if two faces have the same distance, the one with the
      // higher id draws last
      if ((theirDistance > thisDistance)
        || (theirDistance == thisDistance && theirId < localId)) {
        ++myOffset;
      }
    }

    float4 normA = normal[offset + localId * 3];
    float4 normB = normal[offset + localId * 3 + 1];
    float4 normC = normal[offset + localId * 3 + 2];

    normalout[outOffset + myOffset * 3    ] = rotate_vec(normA, orientation);
    normalout[outOffset + myOffset * 3 + 1] = rotate_vec(normB, orientation);
    normalout[outOffset + myOffset * 3 + 2] = rotate_vec(normC, orientation);

    #if UNDO_VANILLA_SHADING
    if ((((int)thisrvA.w) >> 20 & 1) == 0) {
        if (fast_length(normA) == 0) {
            // Compute flat normal if necessary, and rotate it back to match unrotated normals
            float3 N = cross(convert_float3(thisrvA.xyz - thisrvB.xyz), convert_float3(thisrvA.xyz - thisrvC.xyz));
            normA = normB = normC = (float4) (N, 1.f);
        }
        undoVanillaShading(&thisrvA, normA.xyz);
        undoVanillaShading(&thisrvB, normB.xyz);
        undoVanillaShading(&thisrvC, normC.xyz);
    }
    #endif

    thisrvA += pos;
    thisrvB += pos;
    thisrvC += pos;

    // apply hillskew
    int plane = (flags >> 24) & 3;
    int hillskew = (flags >> 26) & 1;
    thisrvA = hillskew_vertex(tileHeightMap, thisrvA, hillskew, minfo.y, plane);
    thisrvB = hillskew_vertex(tileHeightMap, thisrvB, hillskew, minfo.y, plane);
    thisrvC = hillskew_vertex(tileHeightMap, thisrvC, hillskew, minfo.y, plane);

    // position vertices in scene and write to out buffer
    vout[outOffset + myOffset * 3]     = thisrvA;
    vout[outOffset + myOffset * 3 + 1] = thisrvB;
    vout[outOffset + myOffset * 3 + 2] = thisrvC;

    float4 uvA = (float4)(0);
    float4 uvB = (float4)(0);
    float4 uvC = (float4)(0);

    if (uvOffset >= 0) {
      uvA = uv[uvOffset + localId * 3];
      uvB = uv[uvOffset + localId * 3 + 1];
      uvC = uv[uvOffset + localId * 3 + 2];

      if ((((int)uvA.w) >> MATERIAL_FLAG_VANILLA_UVS & 1) == 1) {
        // Rotate the texture triangles to match model orientation
        uvA = rotate_vec(uvA, orientation);
        uvB = rotate_vec(uvB, orientation);
        uvC = rotate_vec(uvC, orientation);

        // Shift texture triangles to world space
        float3 modelPos = convert_float3(pos.xyz);
        uvA.xyz += modelPos;
        uvB.xyz += modelPos;
        uvC.xyz += modelPos;
      }
    }

    uvout[outOffset + myOffset * 3]     = uvA;
    uvout[outOffset + myOffset * 3 + 1] = uvB;
    uvout[outOffset + myOffset * 3 + 2] = uvC;
  }
}
