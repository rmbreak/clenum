import org.lwjgl.*;
import org.lwjgl.opencl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.InfoUtil.*;
import static org.lwjgl.opencl.KHRICD.*;
import static org.lwjgl.system.MemoryStack.*;

class CLDevice {
    private final long device_id;
    private final String DEVICE_NAME;
    private final String DEVICE_VERSION;
    private final String DRIVER_VERSION;
    private final long DEVICE_MAX_COMPUTE_UNITS;

    public CLDevice(long device) {
        this.device_id = device;
        this.DEVICE_NAME = getDeviceInfoStringUTF8(device, CL_DEVICE_NAME);
        this.DEVICE_VERSION = getDeviceInfoStringUTF8(device, CL_DEVICE_VERSION);
        this.DRIVER_VERSION = getDeviceInfoStringUTF8(device, CL_DRIVER_VERSION);
        this.DEVICE_MAX_COMPUTE_UNITS = getDeviceInfoInt(device, CL_DEVICE_MAX_COMPUTE_UNITS) & 0xffffffffL;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Device [0x%x]\n", device_id));
        sb.append(String.format("  Name             : %s\n", DEVICE_NAME));
        sb.append(String.format("  Version          : %s\n", DEVICE_VERSION));
        sb.append(String.format("  Driver Version   : %s\n", DRIVER_VERSION));
        sb.append(String.format("  Max Compute Units: %s\n", DEVICE_MAX_COMPUTE_UNITS));

        return sb.toString();
    }
}

class CLPlatform {
    private final long platform_id;
    private final CLCapabilities capabilities;
    private final String PLATFORM_PROFILE;
    private final String PLATFORM_VERSION;
    private final String PLATFORM_NAME;
    private final String PLATFORM_VENDOR;
    private final String PLATFORM_EXTENSIONS;
    private final Optional<String> PLATFORM_ICD_SUFFIX_KHR;

    public CLPlatform(long platform) {
        this.platform_id = platform;
        this.capabilities = CL.createPlatformCapabilities(platform);
        this.PLATFORM_PROFILE = getPlatformInfoStringUTF8(platform, CL_PLATFORM_PROFILE);
        this.PLATFORM_VERSION = getPlatformInfoStringUTF8(platform, CL_PLATFORM_VERSION);
        this.PLATFORM_NAME = getPlatformInfoStringUTF8(platform, CL_PLATFORM_NAME);
        this.PLATFORM_VENDOR = getPlatformInfoStringUTF8(platform, CL_PLATFORM_VENDOR);
        this.PLATFORM_EXTENSIONS = getPlatformInfoStringUTF8(platform, CL_PLATFORM_EXTENSIONS);
        if (this.capabilities.cl_khr_icd) {
            this.PLATFORM_ICD_SUFFIX_KHR = Optional.of(getPlatformInfoStringUTF8(platform, CL_PLATFORM_ICD_SUFFIX_KHR));
        } else {
            this.PLATFORM_ICD_SUFFIX_KHR = Optional.empty();
        }
    }

    public long getPlatformID() {
        return this.platform_id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Platform [0x%x]\n", platform_id));
        sb.append(String.format("  Profile   : %s\n", PLATFORM_PROFILE));
        sb.append(String.format("  Version   : %s\n", PLATFORM_VERSION));
        sb.append(String.format("  Name      : %s\n", PLATFORM_NAME));
        sb.append(String.format("  Vendor    : %s\n", PLATFORM_VENDOR));
        sb.append(String.format("  Extensions:\n"));
        String extensions = Arrays.stream(PLATFORM_EXTENSIONS.split(" "))
                .map(s -> String.format("    - %s", s))
                .reduce("", (s, ext) -> s + ext + "\n");
        sb.append(extensions);
        if (PLATFORM_ICD_SUFFIX_KHR.isPresent()) {
            sb.append(String.format("  ICD Suffix KHR: %s\n", PLATFORM_ICD_SUFFIX_KHR));
        }

        return sb.toString();
    }
}

class CLEnum {
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

public class Main {
    public static void main(String args[]) {
        CLPlatform[] platforms = CLEnum.getPlatforms();
        for (CLPlatform platform : platforms) {
            System.out.print(platform);

            System.out.println("  Devices:");
            CLDevice[] devices = CLEnum.getDevices(platform);
            for (CLDevice device : devices) {
                String deviceString = Stream.of(device.toString().split("\n"))
                    .map(line -> String.format("    %s", line))
                    .reduce("", (s, n) -> s + n + "\n");
                System.out.print(deviceString);
            }
            System.out.println();
        }
    }
}
