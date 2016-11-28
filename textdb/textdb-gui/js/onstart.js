/*
	Gui <---> Flowchart.js Communication
	GUIJSON <---> TEXTDBJSON Conversion
	
	@Author: Jimmy Wang
*/

// main operation (holds all the methods for the buttons and holds the buttons itself)
var setup = function(){
	var data = {};

	// Apply the plugin on a standard, empty div...
	$('#the-flowchart').flowchart({
		data: data
	});
	
	var operatorI = 0;
	
	var DEFAULT_REGEX = "zika\s*(virus|fever)";
	var DEFAULT_ATTRIBUTES = "first name, last name";
	var DEFAULT_LIMIT = 10;
	var DEFAULT_OFFSET = 5;
	
	/*
		Helper Functions
	*/
	
	//Create Operator Helper Function 
	function getExtraOperators(userInput, panel){
	  var extraOperators = {};
	  
	  if (panel == 'regex-panel'){
		if (userInput == null || userInput == ''){
			userInput = DEFAULT_REGEX;
		}
	    extraOperators['regex'] = userInput;
	  }
	  return extraOperators;
	};
	
	//Create Operator Helper Function 
	function getAttr(panel, keyword){
		var result = $('#' + panel + keyword).val();
		if(keyword == ' .limit'){
			if (result == null || result == ''){
				result = DEFAULT_LIMIT;
			}
		}
		else if(keyword == ' .offset'){
			if (result == null || result == ''){
				result = DEFAULT_OFFSET;
			}
		}
		else if(keyword == ' .attributes'){
			if (result == null || result == ''){
				result = DEFAULT_ATTRIBUTES;
			}
		}
		return result;
	};

	/*
		Button functions
	*/
	
	//Create Operator to send to flowchart.js and display on flowchart
	var createOperator = function(buttonPanel){
		var panel = $(buttonPanel).attr('rel');

		var userInput = $('#' + panel + ' .value').val();	  
		var extraOperators = getExtraOperators(userInput,panel);

		var userLimit = getAttr(panel, ' .limit');
		var userOffset = getAttr(panel, ' .offset');
		var userAttributes = getAttr(panel, ' .attributes');
		var operatorName = $('#' + panel + ' button').attr('id');
		var operatorId = operatorName + '_' + operatorI;
		var operatorData = {
			top: 60,
			left: 500,
			properties: {
				title: (operatorName),
				inputs: {
					input_1: {
						label: 'Input 1',
					}
				},
				outputs: {
					output_1: {
						label: 'Output 1',
					}
				},
				attributes: {
					operator_id: (operatorId),
					operator_type: (operatorName),
				}
			}
		};

		for(var extraOperator in extraOperators){
		  operatorData.properties.attributes[extraOperator] = extraOperators[extraOperator];
		}
		operatorData.properties.attributes['attributes'] = userAttributes;
		operatorData.properties.attributes['limit'] = userLimit;
		operatorData.properties.attributes['offset'] = userOffset;

		operatorI++;

		$('#the-flowchart').flowchart('createOperator', operatorId, operatorData);

		data = $('#the-flowchart').flowchart('getData'); 
	};
	
	//Deletes the selected operator from flowchart.js and GUI
	var deleteOperator = function(){
		$('#the-flowchart').flowchart('deleteSelected');
		data = $('#the-flowchart').flowchart('getData');
	};
	
	/*
		Buttons
	*/

	//Create Operator button. Calls createOperator function
    $('.create-operator').click(function() {
		createOperator(this);
    });
		
	//Delete Operator Button. Calls deleteOperator function
	$('.nav').on('click', '.delete-operator', deleteOperator);
};

$(document).ready(setup);
