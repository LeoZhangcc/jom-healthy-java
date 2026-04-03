package com.jom.healthy.generator.generator.mall;


import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.jom.healthy.generator.generator.base.model.ContextParam;
import com.jom.healthy.generator.generator.mall.controller.MallControllerGenerator;
import com.jom.healthy.generator.generator.mall.mybatisplus.MallMpGenerator;
import com.jom.healthy.generator.generator.restful.mybatisplus.param.MpParam;

import java.util.List;
import java.util.Map;

/**
 * 测试的执行器
 *
 * @author di
 * @date 2018-12-18-6:39 PM
 */
public class MallExecutor {

    /**
     * 默认的生成器
     *
     * @author di
     * @Date 2019/1/13 22:18
     */
    public static void executor(ContextParam contextParam, MpParam mpContext) {

        //执行mp的代码生成，生成entity,dao,service,model，生成后保留数据库元数据
        MallMpGenerator mallMpGenerator = new MallMpGenerator(mpContext);
        mallMpGenerator.initContext(contextParam);
        mallMpGenerator.doGeneration();

        //获取元数据
        List<TableInfo> tableInfos = mallMpGenerator.getTableInfos();
        Map<String, Map<String, Object>> everyTableContexts = mallMpGenerator.getEveryTableContexts();

        //遍历所有表
        for (TableInfo tableInfo : tableInfos) {
            Map<String, Object> map = everyTableContexts.get(tableInfo.getName());

            //生成控制器
            MallControllerGenerator mallControllerGenerator = new MallControllerGenerator(map);
            mallControllerGenerator.initContext(contextParam);
            mallControllerGenerator.doGeneration();

//            //生成主页面html
//            MallPageIndexGenerator mallPageIndexGenerator = new MallPageIndexGenerator(map);
//            mallPageIndexGenerator.initContext(contextParam);
//            mallPageIndexGenerator.doGeneration();
//
//            //生成主页面js
//            mallPageIndexJsGenerator mallPageIndexJsGenerator = new mallPageIndexJsGenerator(map);
//            mallPageIndexJsGenerator.initContext(contextParam);
//            mallPageIndexJsGenerator.doGeneration();

            /*
            //前端使用的是avue+element 没有添加和编辑页面
            //生成添加页面html
            MallPageAddGenerator mallPageAddGenerator = new MallPageAddGenerator(map);
            mallPageAddGenerator.initContext(contextParam);
            mallPageAddGenerator.doGeneration();

            //生成添加页面的js
            MallPageAddJsGenerator mallPageAddJsGenerator = new MallPageAddJsGenerator(map);
            mallPageAddJsGenerator.initContext(contextParam);
            mallPageAddJsGenerator.doGeneration();

            //生成编辑页面html
            MallPageEditGenerator mallPageEditGenerator = new MallPageEditGenerator(map);
            mallPageEditGenerator.initContext(contextParam);
            mallPageEditGenerator.doGeneration();

            //生成编辑页面的js
            MallPageEditJsGenerator mallPageEditJsGenerator = new MallPageEditJsGenerator(map);
            mallPageEditJsGenerator.initContext(contextParam);
            mallPageEditJsGenerator.doGeneration();
            */


//            //生成菜单的sql
//            MallMenuSqlGenerator mallMenuSqlGenerator = new MallMenuSqlGenerator(map);
//            mallMenuSqlGenerator.initContext(contextParam);
//            mallMenuSqlGenerator.doGeneration();
        }
    }

    public static void main(String[] args) {

        ContextParam contextParam = new ContextParam();

        contextParam.setJdbcDriver("com.mysql.jdbc.Driver");
        contextParam.setJdbcUserName("avnadmin");
        contextParam.setJdbcPassword("");
        contextParam.setJdbcUrl("");
        contextParam.setOutputPath("D:\\A Java project\\jom-healthy-java");
        contextParam.setAuthor("ZXJ");
        contextParam.setProPackage("src.main.java.com.jom.healthy");

        MpParam mpContextParam = new MpParam();
        mpContextParam.setGeneratorInterface(true);
        mpContextParam.setIncludeTables(new String[]{"food"});
        mpContextParam.setRemoveTablePrefix(new String[]{""});

        MallExecutor.executor(contextParam, mpContextParam);
    }

}
