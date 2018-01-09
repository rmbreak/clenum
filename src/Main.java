import org.lwjgl.*;
import org.lwjgl.opencl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Arrays;
import java.util.Optional;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.InfoUtil.*;
import static org.lwjgl.opencl.KHRICD.*;
import static org.lwjgl.system.MemoryStack.*;

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
    public CLPlatform[] getPlatforms() {
        MemoryStack stack = stackPush();

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

public class Main {
    public static void main(String args[]) {
        CLEnum enumerator = new CLEnum();
        CLPlatform[] platforms = enumerator.getPlatforms();
        for (CLPlatform platform : platforms) {
            System.out.println(platform);
        }
    }
}
