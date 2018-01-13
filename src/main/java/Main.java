import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Stream;

import opencl.*;

class VecAddProgram extends CLProgram {
    VecAddProgram(CLContext context) throws Exception {
        super(context);
    }

    @Override
    protected String getSource() {
        try {
            return Main.readKernelSource("add.cl");
        } catch (IOException e) {
            return "";
        }
    }

    public int[] vadd(int a[], int b[]) {
        throw new UnsupportedOperationException("Not implemented.");
    }
}

public class Main {
    public static String readKernelSource(String filename) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(classLoader.getResource("kernel/" + filename).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static void demoRunVADD(CLDevice device) {
        try {
            CLContext context = new CLContext(device);
            CLProgram program = new VecAddProgram(context);
            // TODO: setup command queue, create buffers, then finally call the kernel!
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
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
            demoRunVADD(gpuDevice.get());
        } else {
            System.err.println("No OpenCL compatible GPU found.");
        }
    }
}
