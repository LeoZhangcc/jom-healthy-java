package com.jom.healthy.generator.generator.mall.html;

import com.jom.healthy.generator.generator.base.AbstractCustomGenerator;
import org.beetl.core.Template;

import java.io.File;
import java.util.Map;

/**
 * Mall主页面生成器
 *
 * @author di
 * @date 2018-12-13-2:20 PM
 */
public class MallPageIndexGenerator extends AbstractCustomGenerator {

    public MallPageIndexGenerator(Map<String, Object> tableContext) {
        super(tableContext);
    }

    @Override
    public void bindingOthers(Template template) {
        super.bindingInputsParams(template);
    }
    @Override
    public String getTemplateResourcePath() {
        return "/mallTemplates/page.html.btl";
    }

    @Override
    public String getGenerateFilePath() {
        String lowerEntity = (String) this.tableContext.get("lowerEntity");
        File file = new File(contextParam.getOutputPath() + "/html/" + lowerEntity + "/" + lowerEntity + ".vue");
        return file.getAbsolutePath();
    }
}
