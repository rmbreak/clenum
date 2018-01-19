package opencl;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;

abstract public class CLProgram {
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

    public class CLCompileException extends Exception {
		private static final long serialVersionUID = 8395514257861157540L;
		public CompileResult result;

        public CLCompileException(CompileResult result) {
            super(result.toString());
            this.result = result;
        }
    }

    protected final CLContext context;
    protected long program;
    protected long queue;

    public CLProgram(CLContext context) throws CLCompileException,Exception {
        this.context = context;

        String source = getSource();
        this.program = clCreateProgramWithSource(context.getContextID(), source, null);
        if (this.program != 0) {
            StringBuilder options = new StringBuilder("");
            CompileResult result = CompileResult.fromInt(clBuildProgram(program, context.getDevice().getDeviceID(), options, null, NULL));
            if (result != CompileResult.SUCCESS) {
                throw new CLCompileException(result);
            } else {
                this.queue = clCreateCommandQueue(context.getContextID(), context.getDevice().getDeviceID(), 0, (IntBuffer)null);
            }
        } else {
            throw new Exception("Failed to create program.");
        }
    }

    protected abstract String getSource();

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Program [0x%x]", program));

        return sb.toString();
    }
}
