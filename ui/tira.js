window.onload = function(){tira = new Tira();tira.renderPage();}

function Tira()
{
  //vars program, record, and result are available from script element in the html page.
    var _this = this;
    var $form = $("<form id='form' action=''><ul class='config' id='config'/><input id='query' type='button' value='Search'/><input id='execute' type='button' value='Execute'></form>");
    $form.submit(function(){$(':file').attr("disabled",true)});
    $('#tira').append("<div id='info'/>").append($form).append("<div id='results'/>");
    $('#query').click(function(){$form.attr("method","get");$form.submit();});
    var $execute = $('#execute').click(function(){$form.attr("method","post");$form.submit();});
    var $info = $('#info');
    var $config = $('#config');
    var $results = $('#results');

    this.checkExecutable = function()
    {
        $execute.attr("disabled",false);
        var EXECUTABLE = true;
        $(':text,select').each(function(EXECUTABLE){
            //alert($(this).val());
            if($(this).val()===""){$execute.attr("disabled",true);}
        });
    }
    
    this.checkRegex = function( $input, regexString ) {
    	
    	if( regexString != null && regexString != "" ) {
    		
    		var exp = new RegExp( regexString );
    		var value = $input.val();
    		
    		if( !exp.test( value ) ) {
    			$execute.attr("disabled",true);
    			$input.addClass( "regex-invalid" );
    		}
    		else {
    			$execute.attr("disabled",false);
    			$input.removeClass( "regex-invalid" );
    		}
    		
    	}
    	_this.checkExecutable();
    	
    }
    
    this.renderPage = function()
    {
        //_this.loadInfo($info);
        _this.renderRecordEntry("MAIN", $config);
        _this.initAddRemoveButtons();
        _this.checkExecutable();
        _this.renderResultTable($results);
    }
    
    this.loadInfo = function($htmlElement)
    {
        $htmlElement.html(info);
    }
    
    this.renderRecordEntry = function(entryName, $htmlElement)
    {
        var recordEntry = record[entryName];
        var configValues = [""];
        if(config[entryName])
        {
            configValues = config[entryName];
            if(typeof configValues == 'string')
            {configValues= [configValues];}           
        }
        
        for(i in configValues)
        {
            configValue = configValues[i];
            if(typeof recordEntry == 'string' && entryName != entryName.toUpperCase())
            {
                _this.renderInputText(entryName,entryName,configValue,$htmlElement,recordEntry);
            }
            else if(typeof recordEntry == 'object' && (recordEntry instanceof Array))
            {
                _this.renderInputSelect(entryName,entryName,recordEntry,configValue,$htmlElement);
            }
            else if(typeof recordEntry == 'object' && recordEntry.ui)
            {
                switch(recordEntry.ui) {
                    case "input-text": 
                        _this.renderInputText(entryName,configValue,$htmlElement);
                        break;
                    case "input-select": 
                        _this.renderInputSelect(entryName,recordEntry.label,recordEntry.options,configValue,$htmlElement);
                        break;
                    case "input-file": 
                        _this.renderInputFile(entryName,configValue,recordEntry,$htmlElement);
                        break;
                }
            }
            else if(configValue.indexOf("$") > -1)
            {
                _this.renderChildren(configValue,$htmlElement);
            }        
        }
    }
    
    this.renderInputText = function(entryName,paramLabel,valueString,$htmlElement, recordEntry)
    {
    	if( recordEntry == undefined ) {
    		recordEntry = null;
    	}
        var $li = $(document.createElement('li'));
        var $label = $(document.createElement('label'));
        $label.text(paramLabel);
        $li.append($label);    
        var $input = $('<input type="text"/>').attr("name",entryName);
        $input.val(valueString);
        $input.change(function(){
        	_this.checkRegex( $(this),recordEntry );
    	});
        $input.blur(function(){
        	_this.checkRegex( $(this),recordEntry );
    	});
        $label.append($input);
        $htmlElement.append($li);
    }
    
    this.renderInputSelect = function(entryName,paramLabel,options,valueString,$htmlElement)
    {
        var $li = $(document.createElement('li'));
        var $label = $(document.createElement('label'));
        $label.text(paramLabel);
        $li.append($label);    
        var $input = $(document.createElement('select')).attr("name",entryName);
        $input.append($(document.createElement('option')).text('-').val(""));
        for(var o in options) {
             var opt = options[o];
             var $option = $(document.createElement('option'));
             if(opt == valueString){$option.attr("selected","selected");}
             $option.val(opt);
             $option.text(opt);
             if(opt[0] === '$')
             {
                 if(record[opt.substring(1)]){
                     $option.text(record[opt.substring(1)].label);
                 }
             }
             $input.append($option);
        }
        $label.append($input);
        $htmlElement.append($li);
        $input.change( function(event) {
            var $li = $(this).parents('li');
            $li.find('ul').remove();
            var value = $(this).val();
            if(value[0] === "$")
            {
                var $ul = $(document.createElement('ul')).appendTo($li);
                _this.renderRecordEntry(value.substring(1),$ul);
                _this.appendAddButton( $ul.find( 'li:first-child' ) );
            }
            _this.checkExecutable();
        });
        $input.change();       
    }
    
    this.renderInputFile = function(entryName,valueString,recordEntry,$htmlElement)
    {
        var $li = $(document.createElement('li'));         
        var img = document.createElement('img');
        img.setAttribute('src', '/web/icons/dot.png');
        $li.append(img);
        var $label = $(document.createElement('label'));
        $label.text(recordEntry.label);
        $li.append($label);    
        var $input = $('<input type="text"/>').attr("name",entryName);
        $input.val(valueString);
        $label.append($input);
        $input.change(function(){_this.checkExecutable();});
        
        $upload = $(document.createElement('div')).css("display","inline-block").appendTo($li);
        var uploader = new qq.FileUploader({
            // pass the dom node (ex. $(selector)[0] for jQuery users)
            element: $upload[0],
            // path to server-side upload script
            action: '/upload',
            multiple: false,
            // events         
            // you can return false to abort submit
            onSubmit: function(id, fileName){},
            onComplete: function(id, fileName, responseJSON)
            {
                $input.val(responseJSON.filepath);
            }            
        });      
        
        $htmlElement.append($li);   
    }
    
    this.renderChildren = function(value, htmlElement)
    {
        for(var key in record)
        {
            //var key=record.MAIN.params[i];
            if(value.indexOf("$"+key)!=-1){_this.renderRecordEntry(key,htmlElement);}
        }  
    }
    
    this.initAddRemoveButtons = function()
    {
    	// code for having add/remove buttons
        $config = $('#config');
		$config_li = $config.find('li');
		
		$config_li.each( function() {
			
			_this.appendAddButton( $(this) );
			
		} );
		
		// using on to replace the live click handler as live is deprecated
		$( document ).on( "click", "#config input.add-button", function(){
			
			$add_button = $(this);
			$current_li = $( $add_button.parents( 'li' )[0] );
			
			$new_li = $current_li.clone(true);
			
			var $select = $new_li.find('select');
			
			// If select input is being cloned
			if( $select.length > 0 ) {
				$select.val("");
				var $ul = $select.parents('li').find('ul');
				if( $ul.length > 0 )
					$ul.remove();
			}
			// otherwise text input
			else {
				$new_li.find('input[type="text"]').val("");
			}
			
			
			
			$new_li.find('.add-button')
				   .removeClass('add-button')
				   .addClass('remove-button')
				   .val('-');
			
			$new_li.insertAfter($current_li);
		} );
		
		
		
		$( document ).on( "click", "#config input.remove-button", function(){
			
			$( $(this).parents('li')[0] ).remove();
			
		});
    }
    
    this.appendAddButton = function( parent_li ) {
    	
    	$add_button = $('<input type="button" class="add-button" value="+" />');
		
    	$(parent_li).append( $add_button );
    }
    
    this.renderResultTable = function(htmlElement)
    { 
        var $table = $(document.createElement('table')).attr('class','tablesorter').appendTo(htmlElement);        
        var $head = $(document.createElement('thead')).appendTo($table);
        var $header = $(document.createElement('tr')).appendTo($head);
        var $body = $(document.createElement('tbody')).appendTo($table);
        
        var tableHeaders = [];
        
        for(var i in results)
        {
        	result = results[i];
        	for(var key in result)
        	{
        		if(key == key.toUpperCase()){continue;}
        		if(tableHeaders.indexOf(key)<0){tableHeaders.push(key);$header.append($("<th>"+key+"</th>"));}  		
        	}
        }
        
        for(var i in results)
        {
            var result = results[i];
            var $row = $(document.createElement('tr')).appendTo($body);
            for(var j in tableHeaders)
            {                
                var key = tableHeaders[j];
                if(result[key] && result[key].indexOf("http://") == 0)
                {
                    $row.append($("<td><a href='"+result[key]+"'>"+key+"</a></td>"));
                }
                else if (result[key] && result[key] != undefined) 
                {
                    $row.append($("<td>"+result[key]+"</td>"));
                }
                else {$row.append($("<td></td>"));}
            }
        }
        
        $table.columnCollapse();
        $table.tablesorter();
        
        
    }
}