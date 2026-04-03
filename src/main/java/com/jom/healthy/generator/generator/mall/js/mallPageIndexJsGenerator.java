package com.jom.healthy.generator.generator.mall.js;

import com.jom.healthy.generator.generator.base.AbstractCustomGenerator;
import org.beetl.core.Template;

import java.io.File;
import java.util.Map;

/**
 * Mall主页面js生成器
 *
 * @author di
 * @date 2018-12-13-2:20 PM
 */
public class mallPageIndexJsGenerator extends AbstractCustomGenerator {

    public mallPageIndexJsGenerator(Map<String, Object> tableContext) {
        super(tableContext);
    }

    @Override
    public void bindingOthers(Template template) {
        super.bindingInputsParams(template);
    }
    @Override
    public String getTemplateResourcePath() {
        return "/mallTemplates/page.js.btl";
    }

    @Override
    public String getGenerateFilePath() {
        String lowerEntity = (String) this.tableContext.get("lowerEntity");
        File file = new File(contextParam.getOutputPath() + "/js/" + lowerEntity + "/" + lowerEntity + ".js");
        return file.getAbsolutePath();
    }
}
