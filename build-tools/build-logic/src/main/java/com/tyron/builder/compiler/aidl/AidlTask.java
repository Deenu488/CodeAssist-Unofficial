package com.tyron.builder.compiler.aidl;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.common.util.BinaryExecutor;
import java.io.IOException;

public class AidlTask extends Task<AndroidModule> {

    public AidlTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    private static final String TAG = "compileAidl";

    @Override
    public String getName() {
        return TAG;
    }
    
    @Override
    public void prepare(BuildType type) throws IOException {
        }

    public void run() throws IOException, CompilationFailedException {
            }

   }
