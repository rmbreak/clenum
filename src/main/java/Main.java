import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import opencl.CLContext;
import opencl.CLDevice;
import opencl.CLEnum;
import opencl.CLPlatform;
import opencl.CLProgram;

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
        Random rand = new Random();
        int a[] = IntStream.range(0, 10240).map(__ -> rand.nextInt(500)).toArray();
        int b[] = IntStream.range(0, 10240).map(__ -> rand.nextInt(500)).toArray();
        int c_cpu[] = new int[10240];
        for (int i = 0; i < a.length; i++) {
            c_cpu[i] = a[i] + b[i];
        }

        try {
            CLContext context = new CLContext(device);
            VecAddProgram program = new VecAddProgram(context);
            int c_gpu[] = program.vadd(a, b);
            assert(Arrays.equals(c_cpu, c_gpu));
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
