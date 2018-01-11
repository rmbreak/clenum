__kernel void vadd(__global const int *a, __global const int *b, __global int *c)
{
    // get index of current element
    int i = get_global_id(0);

    c[i] = a[i] + b[i];
}
