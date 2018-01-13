package opencl;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class CLContext {
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
