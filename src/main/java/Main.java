import org.lwjgl.*;
import org.lwjgl.opencl.*;
import org.lwjgl.system.*;

import java.io.File;
import java.io.IOException;
import java.nio.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.InfoUtil.*;
import static org.lwjgl.opencl.KHRICD.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

class CLProgram {
    public enum CompileResult {
        SUCCESS(CL_SUCCESS),
        INVALID_PROGRAM(CL_INVALID_PROGRAM),
        INVALID_VALUE(CL_INVALID_VALUE),
        INVALID_DEVICE(CL_INVALID_DEVICE),
        INVALID_BINARY(CL_INVALID_BINARY),
        INVALID_BUILD_OPTIONS(CL_INVALID_BUILD_OPTIONS),
        INVALID_OPERATION(CL_INVALID_OPERATION),
        COMPILER_NOT_AVAILABLE(CL_COMPILER_NOT_AVAILABLE),
        BUILD_PROGRAM_FAILURE(CL_BUILD_PROGRAM_FAILURE),
        OUT_OF_HOST_MEMORY(CL_OUT_OF_HOST_MEMORY);

        private CompileResult(int result) {}

        public static CompileResult fromInt(int result) {
            switch (result) {
                case CL_SUCCESS: return SUCCESS;
                case CL_INVALID_PROGRAM: return INVALID_PROGRAM;
                case CL_INVALID_VALUE: return INVALID_VALUE;
                case CL_INVALID_DEVICE: return INVALID_DEVICE;
                case CL_INVALID_BINARY: return INVALID_BINARY;
                case CL_INVALID_BUILD_OPTIONS: return INVALID_BUILD_OPTIONS;
                case CL_INVALID_OPERATION: return INVALID_OPERATION;
                case CL_COMPILER_NOT_AVAILABLE: return COMPILER_NOT_AVAILABLE;
                case CL_BUILD_PROGRAM_FAILURE: return BUILD_PROGRAM_FAILURE;
                case CL_OUT_OF_HOST_MEMORY: return OUT_OF_HOST_MEMORY;
                default: throw new IllegalArgumentException();
            }
        }
    }

    private final long program;
    private final CLContext context;
    private boolean compiled = false;

    private CLProgram(long program, CLContext context) {
        this.program = program;
        this.context = context;
    }

    public static Optional<CLProgram> createFromSource(CLContext context, String source) {
        Optional<CLProgram> program = Optional.empty();

        long program_id = clCreateProgramWithSource(context.getContextID(), source, null);
        if (program_id != 0) {
            program = Optional.of(new CLProgram(program_id, context));
        }

        return program;
    }

    public CompileResult compile() {
        StringBuilder options = new StringBuilder("");
        CompileResult result = CompileResult.fromInt(clBuildProgram(program, context.getDevice().getDeviceID(), options, null, NULL));
        if (result == CompileResult.SUCCESS) {
            this.compiled = true;
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Program [0x%x]", program));

        return sb.toString();
    }
}

class CLContext {
    private final long context;
    private final CLDevice device;

    public CLContext(CLDevice device) {
        this.device = device;

        try (MemoryStack stack = stackPush()) {
            PointerBuffer ctxProps = stack.mallocPointer(3);
            ctxProps.put(0, CL_CONTEXT_PLATFORM)
                    .put(1, device.getPlatformID())
                    .put(2, 0);
            context = clCreateContext(ctxProps, device.getDeviceID(), null, NULL, null);
        }
    }

    public long getContextID() {
        return context;
    }

    public CLDevice getDevice() {
        return this.device;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Context [0x%x]\n", context));
        sb.append(String.format("  Device [0x%x]", device.getDeviceID()));

        return sb.toString();
    }
}

class CLDevice {
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
            sb.append(String.format("  ICD Suffix KHR: %s", PLATFORM_ICD_SUFFIX_KHR));
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
    public static String readKernelSource(String filename) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(classLoader.getResource("kernel/" + filename).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static void demoRunVADD(CLDevice device, String source) {
        CLContext context = new CLContext(device);
        Optional<CLProgram> program = CLProgram.createFromSource(context, source);
        if (program.isPresent()) {
            CLProgram.CompileResult result =  program.get().compile();
            if (result == CLProgram.CompileResult.SUCCESS) {
                // TODO: setup command queue, create buffers, then finally call the kernel!
            } else {
                System.err.println("Failed to compile program");
            }
        } else {
            System.err.println("Failed to create program from source");
        }
    }

    public static void main(String args[]) throws IOException {
        String sourcecode = readKernelSource("add.cl");
        // Device to run opencl program on
        Optional<CLDevice> gpuDevice = Optional.empty();

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

                if (device.isGPU()) {
                    gpuDevice = Optional.of(device);
                }
            }
            System.out.println();
            System.out.flush();
        }

        if (gpuDevice.isPresent()) {
            demoRunVADD(gpuDevice.get(), sourcecode);
        } else {
            System.err.println("No OpenCL compatible GPU found.");
        }
    }
}
