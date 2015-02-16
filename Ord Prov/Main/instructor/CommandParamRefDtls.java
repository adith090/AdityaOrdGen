package com.m1.bcc.spl.instructor;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;

import org.w3c.dom.Document;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 ******************************************************************************/

public class CommandParamRefDtls {

	TALogger taLogger = TALogger.getTALogger();
	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;
	List<HandlerVariables> cmdtransDtlsList;


	public List<HandlerVariables> getParamValues(String cmdRefId, List<HandlerVariables> paramList, XPath xpath, Document doc, JdbcDatabaseDAO jdbcDatabaseDAO) throws SPLExceptionHandler {

		HandlerVariables handlerVar = new HandlerVariables();
		/* cmdtransDtls - This is the list of cmd parameters and value to be returned to the Handler
		to be inserted into the t_om_cmd_trans_dtls table*/
		cmdtransDtlsList = new ArrayList<HandlerVariables>();

		HashMap <String, HandlerVariables> cmdtransDtlsMap = new HashMap <String, HandlerVariables>();

		/* paramVariableList - This is the list of handler variables that is specific to the param_name */
		List<HandlerVariables> paramVariableList;
		//HandlerVariables paramVariable;

		String concatenateVariable="";
		String functionVariable="";
		String paramName = "";
		HandlerVariables var = new HandlerVariables();;

		try{


			var = paramList.get(0);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] cmdRefId for getting the cmdParamRefList "+cmdRefId, loggercategory);

			// Passing srcTransId, transId and rowId for logging
			List<Map<String, Object>> cmdParamRefList = jdbcDatabaseDAO.getCmdParamRefValues(cmdRefId, var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID());


			for(Object list:cmdParamRefList){
				/* for each param_name in c_om_cmd_param_ref
				 * get the list of handlervariables and perform the concatenate and any special functions.
				 *
				 * Add the end result (handlervarible) to the cmdtransDtlsList
				 */

				Map map = (Map) list;
				concatenateVariable = (String) map.get("CONCATENATE");
				functionVariable = (String) map.get("SPL_FUNC_REF_ID");
				paramName = (String) map.get("PARAM_NAME");

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] paramName in cmdParamRefList: "+paramName, loggercategory);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] concatenate in cmdParamRefList: "+concatenateVariable, loggercategory);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] splFunction in cmdParamRefList: "+functionVariable, loggercategory);

				paramVariableList = new ArrayList<HandlerVariables>();

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] splFunction in cmdParamRefList: "+paramList.size(), loggercategory);
				paramVariableList = getHandlerVariablesbyParamName(paramList,paramName);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] splFunction in cmdParamRefList: "+paramList.size(), loggercategory);

				// If functionVariable is not null,
				// pass the paramVariableList to the special function wrapper which will call the individual functions.
				if(functionVariable != null){

					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] calling getNewParamValuesforSplFunction for splFuncVariable : "+functionVariable, loggercategory);
					paramVariableList = getNewParamValuesforSplFunction(paramVariableList, functionVariable);
				}

				// If concatenateVariable is not null,
				// pass the paramVariableList to the concatenate function.
				if (concatenateVariable != null){

					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] calling getNewParamValuesforConcatenate for concatenate : "+concatenateVariable, loggercategory);
					paramVariableList = getNewParamValuesforConcatenate(paramVariableList, concatenateVariable);
				}

				// At the end of this loop, there should only be one HandlerVariable in the list.
				// add the handleVariable to the final list to be return to the Handler, to be inserted into the t_om_cmd_trans_dtls table


				if(paramVariableList!=null && paramVariableList.size()>0)
					cmdtransDtlsMap.put(paramName, paramVariableList.get(0));


			}// end for(Object list:cmdParamRefList)

		}catch(Exception e){

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_ERROR, "[getParamValues] inside catch block " +e.getMessage(), loggercategory, e);
			throw new SPLExceptionHandler("Exception in populating cmd trans details for param: " + paramName);
		}



		/*
		 *
		 * Piece of code to create the List of Handler Variables to be passed back to the handler for insertion
		 * - Compare the paramList and the cmdtransDtlsList
		 * - If there are parameters that are missing from the cmdtransDtlsList, add the handlervariable to the list to be returned
		 *
		 *
		 * */

		// Loop your list, if param_name of the handlevaribale existin in the hash map, skip,
		// if it does not exist, insert into new list,
		// at the end of the loop, insert every value of the hash map into the new list

		for(Object cmdList:paramList){

			HandlerVariables hVar = (HandlerVariables) cmdList;
			String pName = hVar.getParamName();

			if(!cmdtransDtlsMap.containsKey(pName)){

				cmdtransDtlsList.add(hVar);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] adding the handler variables parameters to the cmdtransDtlsList for paramName: " +pName, loggercategory);

			}
		}

		Iterator<Entry<String, HandlerVariables>> iterator = cmdtransDtlsMap.entrySet().iterator();

		 while (iterator.hasNext()) {

	        Map.Entry pairs = iterator.next();
	        HandlerVariables variables = (HandlerVariables) pairs.getValue();
	        if(variables!=null && !"remove".equalsIgnoreCase(variables.getParamValue()))
	        	cmdtransDtlsList.add((HandlerVariables) pairs.getValue());
	        taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] adding the handler variables parameters to the cmdtransDtlsList for paramName: " , loggercategory);
	    }


		 taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getParamValues] returning the cmdtransDtlsList: ", loggercategory);

		return cmdtransDtlsList;


	}


	//get the list of HandlerVariables that has the same param_name
	public List<HandlerVariables> getHandlerVariablesbyParamName(List<HandlerVariables> inputParamList, String paramName)
	{
		List<HandlerVariables> paramVariableList = new ArrayList<HandlerVariables>();
		HandlerVariables var;

		var = inputParamList.get(0);

		for(Object variables : inputParamList){

			var = (HandlerVariables) variables;

			if(var.getParamName().equalsIgnoreCase(paramName)){

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getHandlerVariablesbyParamName] getParamName equals paramName" + paramName, loggercategory);
				paramVariableList.add(var);
			}
		}

		taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getHandlerVariablesbyParamName] returning the inputParamList to getParameters method", loggercategory);
		return paramVariableList;
	}

	public List<HandlerVariables> getNewParamValuesforConcatenate(List<HandlerVariables> inputParamList, String concatenateString) throws Exception {


		// return handlervariable where the param_sub is null, and the param_value is the concatenate string
		List<HandlerVariables> concatenateList = new ArrayList<HandlerVariables> ();
		HandlerVariables handVar = new HandlerVariables();

		if(inputParamList!=null && inputParamList.size()!=0) {
			handVar = inputParamList.get(0);
			String concatenateParamValue = concatenateString;
			taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforConcatenate] concatenateString is: " +concatenateString, loggercategory);

			for(Object inputList:inputParamList){

				handVar = (HandlerVariables) inputList;
				String concatenateParamName = handVar.getParamName();
				String concatenateParamSub = handVar.getParamSub();
				String paramValueForConcatenate = handVar.getParamValue();

				taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforConcatenate] concatenateParamName is: " +concatenateParamName, loggercategory);
				taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforConcatenate] concatenateParamSub is: " +concatenateParamSub, loggercategory);


				concatenateParamValue = concatenateParamValue.replace("#"+concatenateParamSub+"#", paramValueForConcatenate);
				taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforConcatenate] final paramValue for concatenation is: " +concatenateParamValue, loggercategory);
			}


			// This works because each Handler Variable will have the same values except for param_Sub and param_value
			// Therefore, we can just add to the concatenate list and return.
			handVar.setParamValue(concatenateParamValue);
			handVar.setParamSub("-");
			taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforConcatenate] adding the concatenate parametres to the list ", loggercategory);

			concatenateList.add(handVar);

			taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforConcatenate] returning the concatenateList" , loggercategory);
		}
		return concatenateList;

	}


	// handles the SPL_FUNC_REF_ID in the c_om_cmd_param_ref table and returns the paramValue for every spl function
	// output of the function is to get a List of Handler Variables that contain only 1 Handler Variable
	@SuppressWarnings("rawtypes")
	public List<HandlerVariables> getNewParamValuesforSplFunction(List<HandlerVariables> inputParamList, String functionVariable) throws Exception {

		List<HandlerVariables> splFunctionList = new ArrayList<HandlerVariables> ();
		HandlerVariables handVar = new HandlerVariables();
		HandlerVariables newHandVar = new HandlerVariables();
		List<HandlerVariables> outputValue;

			handVar = inputParamList.get(0);
			taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforSplFunction] inside getNewParamValuesforSplFunction method", loggercategory);

			Class[] parameters = null;

			// load the Class at runtime
			Class SplFunctions = Class.forName("com.m1.bcc.spl.instructor.SplFunctions");

			Object splCmdParamCustomFnsObj = SplFunctions.newInstance();
			parameters = new Class[1];
			parameters[0] = List.class;

			Method method = SplFunctions.getDeclaredMethod(functionVariable, parameters);

			taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforSplFunction] invoking the method for splFunction:" + functionVariable, loggercategory);

			//return outputvalue as HandlerVariable of your method

			outputValue = (List<HandlerVariables>) method.invoke(splCmdParamCustomFnsObj, inputParamList);

			// the 1st entry of the inputParamList will contain all the information required to be passed back to the Handler
			// since we pass the inputParamList limited to the Param_name


			// However, if inputparamList has more than 1 record, it will be a combination of different xml types
			// e.g inputParamList.get(0).getXMLEleType = Resource
			// e.g inputParamList.get(1).getXMLEleType = LineItemAttribute
			// e.g inputParamList.get(2).getXMLEleType = LineItem
			// therefore, we will not be able decide which element to set, thus we nullify it.

			for (Object handlerVar: outputValue)
			{
				HandlerVariables han = (HandlerVariables) handlerVar;
				taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforSplFunction] the final param value to be inserted in splFunctionList is" + han.getParamValue(), loggercategory);
				splFunctionList.add(han);
			}

		taLogger.log(handVar.getSrcTransId(), handVar.getTrans_Id(),handVar.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNewParamValuesforSplFunction] returning the splFunctionList" , loggercategory);
		return splFunctionList;
	}
}
