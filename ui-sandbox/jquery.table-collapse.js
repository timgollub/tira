(function($, undefined) {
	
	ColumnCollapse = function( table ) {
		
		var $table;
		var $table_TH;
		var count_TH;
		var collapsed_count = 0;
		
		var collapsed_class = "collapsed";
		
		var collapsed_width = 25;
		var per_cell_extra_spacing = 12;	//estimated space taken as padding/margin by each column. Will be used to give the table an absolute width
		
		var table_absolute_width_estimate;
		
		
		var Init = function() {
			
			$table = $(table);
			
			$table_TH = $table.find( "th" );
			count_TH  = $table_TH.length;
			
			
			
			$table_TH.each( function(){
				
				$currentTH = $(this);
				
				$wrapperDiv = $( '<div class="th-inner"></div>' );
				$wrapperDiv.text( $currentTH.text() );
				$wrapperDiv.append( '<a href="javascript:;"></a>' );
				
				$currentTH.text('').append($wrapperDiv);
				
			} );
			
			table_absolute_width_estimate = ( collapsed_width + per_cell_extra_spacing ) * count_TH;
			
			$table.css( { "table-layout" : "fixed" } );
			
			$table.find( "th,td" ).css( { "overflow" : "hidden" } );
			
			$table_TH.find('div.th-inner a').click( function(e) {
				
				e.stopPropagation();
				
				var $current_TH = $(this).parents('th');
				
				if( $current_TH.hasClass( collapsed_class ) ) {
					
					if( collapsed_count == count_TH ) {
						$table.width( "100%" );		// if all are collapsed
					}
					
					$current_TH.width( "auto" );
					$current_TH.removeClass( collapsed_class );
					
					collapsed_count--;
				}
				else {
					if( collapsed_count == count_TH-1 ) {
						$table.width( table_absolute_width_estimate );
					} 
					$current_TH.width( collapsed_width );
					$current_TH.addClass( collapsed_class );
					collapsed_count++;
				}
			});
			
		}
		
		Init();
	};
	
	
	
	$.fn.columnCollapse = function() {

		$(this).each(function(){
			ColumnCollapse( $(this) );
		});
		
	};
})(jQuery);