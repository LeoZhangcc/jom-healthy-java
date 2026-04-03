package com.jom.healthy.generator.generator.mall.js;

import com.jom.healthy.generator.generator.base.AbstractCustomGenerator;
import org.beetl.core.Template;

import java.io.File;
import java.util.Map;

/**
 * Mall编辑页面js生成器
 *
 * @author di
 * @date 2018-12-13-2:20 PM
 */
public class MallPageEditJsGenerator extends AbstractCustomGenerator {

    public MallPageEditJsGenerator(Map<String, Object> tableContext) {
        super(tableContext);
    }

    @Override
    public void bindingOthers(Template template) {
    }

    @Override
    public String getTemplateResourcePath() {
        return "/mallTemplates/page_edit.js.btl";
    }

    @Override
    public String getGenerateFilePath() {
        String lowerEntity = (String) this.tableContext.get("lowerEntity");
        File file = new File(contextParam.getOutputPath() + "/js/" + lowerEntity + "/" + lowerEntity + "_edit.js");
        return file.getAbsolutePath();
    }
}
