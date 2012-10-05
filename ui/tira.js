window.onload = function(){tira = new Tira();tira.renderPage();}

function Tira()
{
  //vars pname, record, config, and result are available from script element in the html page.
    var _this = this;
    var $form = $("<form id='form' action=''><ul class='config' id='config'/><input id='query' type='button' value='Search'/><input id='execute' type='button' value='Execute'></form>");
    $form.submit(function(){$(':file').attr("disabled",true)});
    $('#tira').append("<div id='info'/>").append($form).append("<div id='results'/><div id='extras'/>");
    $('#query').click(function(){$form.attr("method","get");$form.submit();});
    var $execute = $('#execute').click(function(){$form.attr("method","post");$form.submit();});
    var $info = $('#info');
    var $config = $('#config');
    var $results = $('#results');
    var $extras = $('#extras');
    
    var input_types = {};    
    var isAutoUpdate = 0;
    var updateTimer;

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
        //_this.setTitle();
        _this.renderRecordEntry("MAIN", $config);
        _this.initAddRemoveButtons();
        _this.checkExecutable();        
        _this.renderResultTable($results);
        _this.renderAutoUpdateToggleButtons($extras);
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
                        _this.renderInputFile(entryName,recordEntry.label,configValue,$htmlElement);
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
        //if( input_types[paramLabel] != undefined ) {_this.appendAddButton($li,true);}
        //else {input_types[paramLabel] = true; _this.appendAddButton($li);}
        if($htmlElement.find('label').text().indexOf(paramLabel)>-1){_this.appendAddButton($li,true);}
        else {_this.appendAddButton($li);}
        
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
                    // $option.val(record[opt.substring(1)].value);
                 }
             }
             $input.append($option);
        }
        $label.append($input);
        //if( input_types[paramLabel] != undefined ) {_this.appendAddButton($li,true);}
        //else {input_types[paramLabel] = true; _this.appendAddButton($li);}
        if($htmlElement.find('label').text().indexOf(paramLabel)>-1){_this.appendAddButton($li,true);}
        else {_this.appendAddButton($li);}

        $htmlElement.append($li);
        $input.change( function(event) {
            var $li = $(this).parent().parent(); //->label->li
            $li.find('ul').remove();
            var value = $(this).val();

            //if(value[0] === "$")
            if(value.indexOf("$")!=-1)
            {
                var $ul = $(document.createElement('ul')).appendTo($li);
                //_this.renderChildren(value, $ul);                
                _this.renderRecordEntry(value.substring(1),$ul);
                //_this.appendAddButton( $ul.find( 'li:first-child' ) );
                //_this.appendAddButton( $ul.find( 'li' ) );
            }
            
            _this.checkExecutable();
        });
        $input.change();       
    }
    
    this.renderInputFile = function(entryName,paramLabel,valueString,$htmlElement)
    {
        var $li = $(document.createElement('li'));
        var $label = $(document.createElement('label'));
        $label.text(paramLabel);
        $li.append($label); 
        var $input = $('<input type="text"/>').attr("name",entryName);
        $input.val(valueString);
        $label.append($input);
        $input.change(function(){_this.checkExecutable();});
        
        $upload = $(document.createElement('div')).css("display","inline-block").css("vertical-align","bottom").appendTo($li);
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
        //determine parameter order.
        matches=[];
        for(var key in record)
        {
            pos = value.indexOf("$"+key)
            if(pos>-1 && !matches[pos]){matches[pos]=key;}
            else if (pos>-1 && matches[pos])
            {
                alert(matches[pos]+" : "+key);
                if(matches[pos].length<key.length){matches[pos]=key;}
            }
        }
        for(var pos in matches)
        {
            _this.renderRecordEntry(matches[pos],htmlElement);
        }  
    }
    
    this.initAddRemoveButtons = function()
    {
    	// code for having add/remove buttons
  /*      $config = $('#config');
		$config_li = $config.find('li');
		
		var input_types = {};
		
		$config_li.each( function() {
			
			var $li = $(this);
			if($li.children().length<2){
			var type = $li.text().substring(0,4);

			var is_remove_button = false;
			
			if( input_types[type] != undefined ) {
				is_remove_button = true;
			}
			else {
				input_types[type] = true;
			}
			
			_this.appendAddButton( $(this), is_remove_button );
			}
			
		} );
*/
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
    
    this.appendAddButton = function( parent_li, is_remove ) {
    	
    	var $li = $(parent_li);
    	
    	if( $li.children('input[type="button"]').length == 0 ) {
        	
    		$button = $( '<input type="button" />');
    		
    		if( is_remove != true ) {
    			$button.addClass('add-button').val('+');
    		}
    		else {
    			$button.addClass('remove-button').val('-');
    		}
    		
    		var $child_ul = $li.find('ul');
    		
    		if( $child_ul.length ) {
    			$child_ul.before( $button );
    		}
    		else {
            	$li.append( $button );
    		}

    	}
    	
    }
    
    this.renderResultTable = function(htmlElement)
    {
    	// only process if results is not empty
    	if( results != undefined )
    	{
    	
	    	//alert("rendering result table now");
	    	
	        var $table = $(document.createElement('table')).attr('class','tablesorter').appendTo(htmlElement);        
	        var $head = $(document.createElement('thead')).appendTo($table);
	        var $header = $(document.createElement('tr')).appendTo($head);
	        var $body = $(document.createElement('tbody')).appendTo($table);
	        
	        var tableHeaders = [];
	        
	        if(record.OUTPUT && results)
	        {
	            tableHeaders=record.OUTPUT;
	            for(var i in tableHeaders) {$header.append($("<th>"+tableHeaders[i]+"</th>"));}
	        }
	        else
	        {
	            for(var i in results)
	            {
	            	result = results[i];
	            	for(var key in result)
	            	{
	            		if(key == key.toUpperCase()){continue;}
	            		if(tableHeaders.indexOf(key)<0){tableHeaders.push(key);$header.append($("<th>"+key+"</th>"));}  		
	            	}
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
	                else if(result[key] && result[key].indexOf("$") == 0 && record[result[key].substring(1)])
	                {
	                   $row.append($("<td>"+record[result[key].substring(1)].label+"</td>"));
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
    
    
    // This function will render the toggle buttons to choose whether to auto update results or not
    
    this.renderAutoUpdateToggleButtons = function( $htmlElement )
    {
    	if( results )
		{
	    	var $toggleParent   = $( '<div id="toggleParent"><span>Auto Update Result Table</span></div>' );
	    	var $radioButtonOn  = $( '<input type="radio" id="updateOn" name="autoUpdate" value="1" /><label for="updateOn">On</label>' );
	    	var $radioButtonOff = $( '<input type="radio" id="updateOff" name="autoUpdate" value="0" checked="checked" /><label for="updateOff">Off</label>' );
	    	
	    	$toggleParent.append( $radioButtonOn );
	    	$toggleParent.append( $radioButtonOff );
	    	
	    	$toggleParent.appendTo( $htmlElement );
	    	
	    	$( 'input[name="autoUpdate"]' ).change( function(){
	    		
	    		isAutoUpdate = $(this).val();
	    		
	    		//console.log( 'change called' );
	    		
	    		_this.setupAutoTimer();
	    	
	    	} )
	    	
	    	
		}
    }
    
    
    // This function will setup the update function for the given number of seconds
    
    this.setupAutoTimer = function()
    {
    	//console.log( "calling setupAutoTimer" );
    	
    	var interval = 5000;
    	if( isAutoUpdate == 1 ) {
    		
    		//console.log( "rendering results - "+interval );
    		
    		_this.fetchResultsAndSetTimer(interval);
    	}
    	else if( updateTimer ){
    		clearTimeout( updateTimer );
    	}
    }
    
    
    // This function will fetch fresh results from the Server for the current page
    
    this.fetchResultsAndSetTimer = function( interval )
    {
    	var currentUrl = window.location.href;
    	
    	if( currentUrl && currentUrl.indexOf('?') != -1 )
		{
    		var splittedUrl = currentUrl.split('?');
    		var ajaxUrl = splittedUrl[0] + '.json?' + splittedUrl[1];
    		
    		$.getJSON( 
    			ajaxUrl, 
    			function(data){
    				
    				if( data ) {
    					
    					results = data;	// replacing old data with new one
    					
    					$results.find('table').remove();
    					//$results.fadeOut('slow');
    					
    					_this.renderResultTable($results);
    					
    					//$results.fadeIn('slow');
    				}
    				
    				setTimeout( _this.setupAutoTimer, interval );
    			}
			)
    		
		}
    }
}
