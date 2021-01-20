package com.arthas.service.command.basic1000;

import com.arthas.service.shell.command.AnnotatedCommand;
import com.arthas.service.shell.command.Command;
import com.arthas.service.shell.command.CommandProcess;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;
import static com.taobao.text.ui.Element.row;


@Name("default")
@Summary("Display Arthas default")
@Description("Examples:\n" + " default\n" + " default sc\n" + " default sm\n" + " default watch")
public class DefaultCommand  extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        String message = "";
        message = RenderUtil.render(mainHelp(), process.width());
        process.write(message);
        process.end();
    }


    public Element mainHelp(){
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("任务名称").style(Style.style(Decoration.bold)), new LabelElement("任务列表"));
        for(int i = 0 ;i < 5 ;i ++){
            table.add(row().add(label("哈哈").style(Style.style(Color.green))).add(label("结果值")));
        }
        return table;
    }
}
