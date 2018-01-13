package opencl;

import static org.lwjgl.opencl.CL10.CL_PLATFORM_EXTENSIONS;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_NAME;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_PROFILE;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_VENDOR;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_VERSION;
import static org.lwjgl.opencl.InfoUtil.getPlatformInfoStringUTF8;
import static org.lwjgl.opencl.KHRICD.CL_PLATFORM_ICD_SUFFIX_KHR;

import java.util.Arrays;
import java.util.Optional;

import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLCapabilities;

public class CLPlatform {
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
