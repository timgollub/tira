$(function(){
	
	$config = $('#config');
	$config_li = $config.find('li');
	
	$config_li.each( function() {
		
		$add_button = $('<input type="button" class="add-button" value="+" />');
		
		$(this).append( $add_button );
		
	} );
	
	// using on to replace the live click handler as live is deprecated
	$( document ).on( "click", "#config input.add-button", function(){
		
		$add_button = $(this);
		$current_li = $add_button.parents( 'li' );
		
		$new_li = $current_li.clone();
		
		$new_li.find('.add-button')
			   .removeClass('add-button')
			   .addClass('remove-button')
			   .val('-');
		
		$new_li.insertAfter($current_li);
	} );
	
	
	
	$( document ).on( "click", "#config input.remove-button", function(){
		
		$(this).parents('li').remove();
		
	});
	
})
