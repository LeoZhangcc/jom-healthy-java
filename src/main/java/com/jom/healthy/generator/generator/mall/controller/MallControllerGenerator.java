package com.jom.healthy.generator.generator.mall.controller;

import com.jom.healthy.generator.generator.base.AbstractCustomGenerator;
import org.beetl.core.Template;

import java.io.File;
import java.util.Map;

/**
 * 带restful接口控制器生成器
 *
 * @author di
 * @date 2018-12-13-2:20 PM
 */
public class MallControllerGenerator extends AbstractCustomGenerator {

    public MallControllerGenerator(Map<String, Object> tableContext) {
        super(tableContext);
    }

    @Override
    public void bindingOthers(Template template) {
        template.binding("controllerPackage", contextParam.getProPackage().replace("src.main.java.","") + ".controller");
    }

    @Override
    public String getTemplateResourcePath() {
        return "/gunsTemplates/controller.java.btl";
    }

    @Override
    public String getGenerateFilePath() {
        String proPackage = this.contextParam.getProPackage();
        String proPath = proPackage.replaceAll("\\.", "/");
        File file = new File(contextParam.getOutputPath() + "/" + proPath + "/controller/" + tableContext.get("entity") + "Controller.java");
        return file.getAbsolutePath();
    }
}
