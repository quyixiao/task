package com.arthas.service.shell.command.internal;


import com.arthas.service.utils.LogUtil;

import java.io.*;

/**
 * 重定向处理类
 *
 * @author gehui 2017年7月27日 上午11:38:40
 * @author hengyunabc 2019-02-06
 */
public class RedirectHandler extends PlainTextHandler implements CloseFunction {
    private PrintWriter out;

    public RedirectHandler() {

    }

    public RedirectHandler(String name, boolean append) throws IOException {
        File file = new File(name);

        if (file.isDirectory()) {
            throw new IOException(name + ": Is a directory");
        }

        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
        }
        out = new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
    }

    @Override
    public String apply(String data) {
        data = super.apply(data);
        if (out != null) {
            out.write(data);
            out.flush();
        } else {
            LogUtil.getResultLogger().info(data);
        }
        return data;
    }

    @Override
    public void close() {
        if (out != null) {
            out.close();
        }
    }
}
