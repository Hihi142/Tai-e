package pku;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pascal.taie.World;
import pascal.taie.analysis.ClassAnalysis;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JClass;


public class PointerAnalysisTrivial extends ClassAnalysis<PointerAnalysisResult> {
    public static final String ID = "pku-pta-trivial";

    private static final Logger logger = LogManager.getLogger(IRDumper.class);
    private static final String SUFFIX = ".pta";
    private static final String PTA_DIR = "pta";

    /**
     * Directory to dump Result.
     */
    private final File dumpDir;

    public PointerAnalysisTrivial(AnalysisConfig config) {
        super(config);
        dumpDir = new File(World.get().getOptions().getOutputDir(), PTA_DIR);
        if (!dumpDir.exists()) {
            dumpDir.mkdirs();
        }
        logger.info("Dumping PTA result in {}", dumpDir.getAbsolutePath());
    }

    @Override
    public PointerAnalysisResult analyze(JClass jclass) {
        var result = new PointerAnalysisResult();
        var preprocess = new PreprocessResult();
        jclass.getDeclaredMethods().forEach(method->{
            preprocess.analysis(method.getIR());
        });
        
        var objs = new TreeSet<Integer>(preprocess.obj_ids.values());

        preprocess.test_pts.forEach((test_id, pt)->{
            result.put(test_id, objs);
        });
        
        dump(jclass, result);
        
        return result;
    }

    protected void dump(JClass jclass, PointerAnalysisResult result){
        String fileName = jclass.getName() + SUFFIX;
        try (PrintStream out = new PrintStream(new FileOutputStream(
            new File(dumpDir, fileName)))) {
            
            out.println(result);
            
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump class {}", jclass, e);
        }
    }
    
}
