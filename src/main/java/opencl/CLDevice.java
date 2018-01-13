package opencl;

import static org.lwjgl.opencl.CL10.CL_DEVICE_GLOBAL_MEM_CACHE_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_GLOBAL_MEM_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_CLOCK_FREQUENCY;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_COMPUTE_UNITS;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_DEVICE_VERSION;
import static org.lwjgl.opencl.CL10.CL_DRIVER_VERSION;
import static org.lwjgl.opencl.InfoUtil.getDeviceInfoInt;
import static org.lwjgl.opencl.InfoUtil.getDeviceInfoLong;
import static org.lwjgl.opencl.InfoUtil.getDeviceInfoStringUTF8;

public class CLDevice {
    private final long device_id;
    private final long platform_id;
    private final String DEVICE_NAME;
    private final String DEVICE_VERSION;
    private final String DRIVER_VERSION;
    private final long DEVICE_MAX_COMPUTE_UNITS;
    private final long DEVICE_GLOBAL_MEM_SIZE;
    private final long DEVICE_GLOBAL_MEM_CACHE_SIZE;
    private final long DEVICE_MAX_CLOCK_FREQUENCY;
    private final long DEVICE_TYPE;

    public CLDevice(long device) {
        this.device_id = device;
        this.platform_id = getDeviceInfoLong(device, CL_DEVICE_PLATFORM);
        this.DEVICE_NAME = getDeviceInfoStringUTF8(device, CL_DEVICE_NAME);
        this.DEVICE_VERSION = getDeviceInfoStringUTF8(device, CL_DEVICE_VERSION);
        this.DRIVER_VERSION = getDeviceInfoStringUTF8(device, CL_DRIVER_VERSION);
        this.DEVICE_MAX_COMPUTE_UNITS = getDeviceInfoInt(device, CL_DEVICE_MAX_COMPUTE_UNITS) & 0xffffffffL;
        this.DEVICE_GLOBAL_MEM_SIZE = getDeviceInfoLong(device, CL_DEVICE_GLOBAL_MEM_SIZE);
        this.DEVICE_GLOBAL_MEM_CACHE_SIZE = getDeviceInfoLong(device, CL_DEVICE_GLOBAL_MEM_CACHE_SIZE);
        this.DEVICE_MAX_CLOCK_FREQUENCY = getDeviceInfoInt(device, CL_DEVICE_MAX_CLOCK_FREQUENCY) & 0xffffffffL;
        this.DEVICE_TYPE = getDeviceInfoLong(device, CL_DEVICE_TYPE);
    }

    public boolean isGPU() {
        return (this.DEVICE_TYPE & CL_DEVICE_TYPE_GPU) != 0;
    }

    public long getPlatformID() {
        return this.platform_id;
    }

    public long getDeviceID() {
        return this.device_id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Device [0x%x]\n", device_id));
        sb.append(String.format("  Name             : %s\n", DEVICE_NAME));
        sb.append(String.format("  Version          : %s\n", DEVICE_VERSION));
        sb.append(String.format("  Driver Version   : %s\n", DRIVER_VERSION));
        sb.append(String.format("  Max Compute Units: %s\n", DEVICE_MAX_COMPUTE_UNITS));
        sb.append(String.format("  Max Memory       : %s MB\n", DEVICE_GLOBAL_MEM_SIZE / 1024 / 1024));
        sb.append(String.format("  Max Memory Cache : %s KB\n", DEVICE_GLOBAL_MEM_CACHE_SIZE / 1024));
        sb.append(String.format("  Max Clock Freq   : %s MHz", DEVICE_MAX_CLOCK_FREQUENCY));

        return sb.toString();
    }
}
