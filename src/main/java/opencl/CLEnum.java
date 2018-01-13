package opencl;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class CLEnum {
    public static CLPlatform[] getPlatforms() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            clGetPlatformIDs(null, pi);
            CLPlatform[] platforms = new CLPlatform[pi.get(0)];

            if (platforms.length > 0) {
                PointerBuffer platforms_buffer = stack.mallocPointer(platforms.length);
                clGetPlatformIDs(platforms_buffer, (IntBuffer)null);

                for (int i = 0; i < platforms_buffer.capacity(); i++) {
                    long platform = platforms_buffer.get(i);
                    platforms[i] = new CLPlatform(platform);
                }
            }

            return platforms;
        }
    }

    public static CLDevice[] getDevices(CLPlatform platform) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer ndevices = stack.mallocInt(1);
            clGetDeviceIDs(platform.getPlatformID(), CL_DEVICE_TYPE_ALL, null, ndevices);
            CLDevice[] devices = new CLDevice[ndevices.get()];

            if (devices.length > 0) {
                PointerBuffer devices_buffer = stack.mallocPointer(devices.length);
                clGetDeviceIDs(platform.getPlatformID(), CL_DEVICE_TYPE_ALL, devices_buffer, (IntBuffer)null);

                for (int i = 0; i < devices_buffer.capacity(); i++) {
                    long device = devices_buffer.get(i);
                    devices[i] = new CLDevice(device);
                }
            }

            return devices;
        }
    }
}
