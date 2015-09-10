package com.code.generator;

 import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * 
* @ClassName: CodeGenertorUtil 
* @Package com.linkdsp.codehelper 
* @Description: TODO(这里用一句话描述这个类的作用) 
* @author tiebin.zhang
* @date 2014年11月27日 下午2:13:17 
* @version V1.0
 */
public class CodeGeneratorUtil {
	private static String ROOT_PATH="/Users/ymt/workspace_etp/generator"; //代码基本路径
	public static void main(String[] args) throws ResourceNotFoundException, ParseErrorException, Exception {
		
		String tablename =  "pro_product"; //数据库表名
		String classname = "ProPorduct"; //类前置（即：子模块名）
		String moduleName = "pro"; //模块名
		String tableDesc = "系统人员功能管理"; //当前业务模块的概要想描述（表描述）
		generator(tablename,classname,moduleName,tableDesc);
	}
	@SuppressWarnings({ "resource", "rawtypes", "unchecked" })
	private static void generator(String tablename,String classname,String moduleName,String tableDesc)throws  Exception{
		if(classname!=null)
		{
			classname=classname.substring(0,1).toUpperCase()+classname.substring(1);
		}
		//生成 dao层，service层，controll层
		//初始化spring
		BeanFactory beanFactory = new ClassPathXmlApplicationContext("classpath:/com/code/generator/spring-application-ds.xml");
		//获取数据库链接
		SqlSession sqlsession = (SqlSession)beanFactory.getBean("information_schemaSqlsession");
		
		//获取表结构 
		List<Map> data = sqlsession.selectList("com.linkdsp.codehelper.getColumnInfo",tablename);
		System.out.println(data);
		Context context = new VelocityContext();
		context.put("moduleName", moduleName);
		context.put("collist", data);
		context.put("tablename", tablename);
		context.put("classname", classname); 
		context.put("tableDesc", tableDesc);
		context.put("newclassname", classname.substring(0,1).toLowerCase()+classname.substring(1)); 
		//生成实体bean
		for(int i = 0;i<data.size();i++){
			data.get(i).put("newcolumn_name", underlineProccess(data.get(i).get("column_name").toString(),true));
			data.get(i).put("newfield_name", underlineProccess(data.get(i).get("column_name").toString()));
			if(data.get(i).get("column_name").toString().toLowerCase().equals("created_time")||
					data.get(i).get("column_name").toString().toLowerCase().equals("updated_time")){
					data.get(i).put("coltype", "Long");
					continue;
			}
			if(data.get(i).get("data_type").equals("varchar")){
				data.get(i).put("coltype", "String");
				continue;
			}
			String dataType=data.get(i).get("data_type").toString();
			if(dataType.equals("char")){
				data.get(i).put("coltype", "String");
				continue;
			}
			if(dataType.equals("int")){
				data.get(i).put("coltype", "Integer");
				continue;
			}
			if(dataType.equals("bigint")){
				data.get(i).put("coltype", "Long");
				continue;
			}
			if(dataType.equals("double")|| dataType.equals("decimal")){
				data.get(i).put("coltype", "BigDecimal");
				addImport(context,"java.math.BigDecimal");
				//context.put("import", "import java.math.BigDecimal;");
				continue;
			}
			if(dataType.equals("date")){
				data.get(i).put("coltype", "Date");
				addImport(context,"java.util.Date");
				continue;
			}
			if(dataType.equals("datetime")){
				data.get(i).put("coltype", "Date");
				addImport(context,"java.util.Date");
				continue;
			}
			if(dataType.equals("timestamp")){
				data.get(i).put("coltype", "Date");
				addImport(context,"java.util.Date");
				continue;
			}
			if(dataType.equals("tinyint")){
				data.get(i).put("coltype", "Integer"); 
				continue;
			}
		}
		if(context.get("import") == null){
			context.put("import", "");
		}
		//生成实体类
		genertorDomain(context, moduleName, classname);
		//生成Entity
		genertorEntity(context, moduleName, classname);
		//生成实体类
		genertorDomainXML(context, moduleName, classname);
		//生成DAO
		genertorDao(context, moduleName, classname);
		//生成service层
		genertorService(context, moduleName, classname); 
		//生成RESTFulAPI层
		genertorController(context, moduleName, classname);
				
	}
	private static void addImport(Context context,String importConent){
		if(context.get("import")!=null ){
			if(context.get("import").toString().indexOf(importConent)<0)
				context.put("import", context.get("import") + "\nimport "+importConent +";");
		}else{
			context.put("import","import "+importConent +";");
		}
	}
	private static void genertorDomainXML(Context context, String moduleName,
			String classname)throws ResourceNotFoundException, ParseErrorException, Exception   {
		String type = "xml";
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init();
		//生成dao层  
		Template template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		File domainFile = new File(ROOT_PATH+"/ymt-etp-data-service/src/main/java/com/ymt/dao/"+moduleName+"/xml/"+classname+".map.xml");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		System.out.println("generating Domain XML: " + domainFile.getAbsolutePath());
		PrintWriter writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		
		writer.flush();
		writer.close();  
	}
	private static void genertorController(Context context, String moduleName,
			String classname)throws ResourceNotFoundException, ParseErrorException, Exception  {
		String type = "controller";
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init();
		//生成dao层  
		Template template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		File domainFile = new File(ROOT_PATH+"/ymt-etp-resources/src/main/java/com/ymt/etp/rs/resources/"+moduleName+"/"+classname+"Resource.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		PrintWriter writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close();  
		
	}
	private static void genertorService(Context context, String moduleName,
			String classname) throws ResourceNotFoundException, ParseErrorException, Exception {
		String type = "service";
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init();
		//生成dao层  
		Template template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		File domainFile = new File(ROOT_PATH+"/ymt-etp-service/src/main/java/com/ymt/service/"+moduleName+"/I"+classname+"Service.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		PrintWriter writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close(); 
		 type = "service.impl";
		 template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		 domainFile = new File(ROOT_PATH+"/ymt-etp-service/src/main/java/com/ymt/service/"+moduleName+"/impl/"+classname+"ServiceImpl.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		 writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close(); 
	}
	private static void genertorDao(Context context, String moduleName, String classname) throws ResourceNotFoundException, ParseErrorException, Exception {
		String type = "dao";
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init();
		//生成dao层  
		Template template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		File domainFile = new File(ROOT_PATH+"/ymt-etp-data-service/src/main/java/com/ymt/dao/"+moduleName+"/I"+classname+"Dao.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		PrintWriter writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close(); 
		 type = "dao.impl";
		 template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		 domainFile = new File(ROOT_PATH+"/ymt-etp-data-service/src/main/java/com/ymt/dao/"+moduleName+"/impl/"+classname+"DaoImpl.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		 writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close(); 
	}
	public static void genertorDomain(Context context ,String moduleName,String classname )throws ResourceNotFoundException, ParseErrorException, Exception {
		String type = "domain";
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init();
		//生成dao层  
		Template template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		File domainFile = new File(ROOT_PATH+"/ymt-etp-domain/src/main/java/com/ymt/domain/"+moduleName+"/"+classname+"Domain.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		PrintWriter writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close(); 
	}
	public static void genertorEntity(Context context ,String moduleName,String classname )throws ResourceNotFoundException, ParseErrorException, Exception {
		String type = "entity";
		Velocity.setProperty("input.encoding", "utf-8");
		Velocity.setProperty("output.encoding", "utf-8");
		Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");  
		Velocity.init();
		//生成dao层  
		Template template = Velocity.getTemplate("com/code/generator/"+type+".vm"); 
		File domainFile = new File(ROOT_PATH+"/ymt-etp-core/src/main/java/com/ymt/entity/"+moduleName+"/"+classname+"Entity.java");
		if(!domainFile.getParentFile().exists()){
			domainFile.getParentFile().mkdirs();
		}
		PrintWriter writer = new PrintWriter(domainFile,"utf-8");
		template.merge(context, writer);
		writer.flush();
		writer.close(); 
	}
	/**
	 * 
	 * 去掉下划线，
	 * 下划线后面第一个字符大写，
	 * 第一个字符大写
	 */
	public static String underlineProccess(String str,boolean firstToUpperCase){
		if(str!=null){
			str=str.toLowerCase();
			str = strProccess("_",str);
			if(firstToUpperCase)
				str=str.substring(0,1).toUpperCase()+str.substring(1);
			
			return str;
		}
		return null;
	}
	/**
	 * 
	 * 去掉下划线，
	 * 下划线后面第一个字符大写，
	 * 第一个字符大写
	 */
	public static String underlineProccess(String str){
		return underlineProccess(str,false);
	}
	private static String strProccess(String replaceStr,String str){
		if(str!=null){
			int index = str.indexOf(replaceStr);
			if(index>-1){
				String header=str.substring(0,index);
				String tail = str.substring(index+replaceStr.length());
				tail=tail.substring(0,1).toUpperCase()+tail.substring(1);
				str=strProccess(replaceStr,header+tail);
			}
		}
		return str;
	}
	public static void maintest(String[] args) {
		String result = underlineProccess("LAST_UPDATE_LOGIN");
		System.out.println(result);
	}
}
