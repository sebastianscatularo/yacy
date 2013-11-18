YaCyPage.Parts = {};
YaCyPage.validable = []; // store validable elements

YaCyPage.Parts.StartPoint = function() {
	var self = this;
	var root = $('#startPoint');
	this.valid = false;

	// cache some elements
	this.e = {
		crawlingURL: $('#crawlingURL'),
		btnBar: root.find('*[data-id="getSiteData"]'),
		btnRobotsAndStructure: root.find('button[data-id="robotsAndStructure"]'),
		btnRobots: root.find('button[data-id="robots"]'),
		startPointDetails: $('#startPointDetails'),
		startPointSelect: $('#startPointSelect')
	};

	this.pagesInfoLoaded = function() {
    // hide buttons..
    this.e.btnBar.hide();
    // ..and reset
    this.e.btnRobotsAndStructure.prop('disabled', false);
    YaCyUi.Form.Button.switchIcon(this.e.btnRobotsAndStructure);
    YaCyUi.Form.Button.switchText(this.e.btnRobotsAndStructure);
    this.e.btnRobots.prop('disabled', false);
    YaCyUi.Form.Button.switchIcon(this.e.btnRobots);
    YaCyUi.Form.Button.switchText(this.e.btnRobots);
	}

	/** Check entered URLs. Set validation states and buttons for site checking.
  	* @param {jQuery} Element to validate */
	this.validateStartUrls = function(jObj, evObj) {
  	var content = jObj.val().trim();
  	var entries = content === '' ? [] : content.split('\n');
  	var state; // final elements state to return

  	// hide startpoint details & select, url has changed
  	if (evObj != null && evObj.type == 'keyup') {
  		self.e.startPointDetails.hide('slow');
  		self.e.startPointSelect.hide('slow');
  		YaCyPage.CrawlStart.emptyData();
  	}

  	entries = YaCyUi.Tools.cleanStringArray(entries);
  	if (entries.length > 0) {
  	  if (YaCyUi.Tools.Validation.isCrawlerUrl(entries)) {
  	  	// correct entries
  	  	state = {
	   	  	hints: {
	   	  		ok: true,
	   	  		clear: true,
	   	  		help: false,
	   	  		show: true
	   	  	},
	      	validation: {
	      		valid: true
	      	}
	    	};

	    	if (entries.length > 1) {
	      	// URL list
	      	self.e.btnRobots.show();
	      	self.e.btnRobotsAndStructure.hide();
	    	} else {
	      	// single URL
	      	self.e.btnRobots.hide();
	      	self.e.btnRobotsAndStructure.show();
	    	}
	    	self.e.btnBar.show();
  	  } else {
  	  	self.e.btnBar.hide();
  			if (entries.length > 1) {
	      	// multiple entries, but one is not a url
	      	state = {
	      		hints: {
	      	  	error: 'invalid-list',
	      	  	help: true,
	      	  	show: true,
	      	  	clear: true
	      		},
	      		validation: {
	      	  	valid: false
	      		}
	      	};
	    	} else {
	      	// single entry, not a url
		  		state = {
						hints: {
	          	error: 'invalid',
	          	help: true,
	          	show: true,
	          	clear: true
	        	},
	        	validation: {
	          	valid: false
	        	}
	      	};
		  	}
	    }
		} else {
	  	self.e.btnBar.hide();
	  	state = {
	    	hints: {
	      	help: true,
	  	  	show: true,
	  	  	clear: true,
	  	  	error: 'empty'
	  		},
	  		validation: {
	      	valid: false
	    	}
	  	};
	  	if (!jObj.is(':focus')) {
  	  	state.hints.show = false;
  	  }
		}
		self.valid = state.validation.valid === true ? true : false;
		if (self.valid === false) {
			YaCyUi.Form.digOut(jObj.attr('id'));
		}
		return state;
  };

  this.addInteractionHandler = function() {
  	// form validation
  	YaCyPage.validable.push(self.e.crawlingURL); // store validable element
  	YaCyUi.Form.Validate.addValidator(self.e.crawlingURL, {
	  	func: self.validateStartUrls,
	  	delay: YaCyPage.validationDelay,
	  	onload: true
	  });

	  // crawl start point buttons
		self.e.btnRobots.prop('disabled', false).click(function(evObj) {
			evObj.preventDefault();
			YaCyPage.CrawlStart.getPagesInfo(self.pagesInfoLoaded, self);
		});
		self.e.btnRobotsAndStructure.prop('disabled', false).click(function(evObj) {
			evObj.preventDefault();
			$(this).prop('disabled', true);
			YaCyUi.Form.Button.switchIcon($(this), 'icon-loader');
			YaCyUi.Form.Button.switchText($(this), 'Loading..');
			YaCyPage.CrawlStart.getPagesInfo(self.pagesInfoLoaded, self);
		});
  };

  // init
  self.addInteractionHandler();
  return self;
};

YaCyPage.Parts.CrawlerFilter = function() {
	var self = this;
	var root = $('#crawlerFilter');
	this.valid = false;

	this.e = {
		countryMustMatchList: $('#countryMustMatchList')
	};

	/** Validate country code filter input.
	  * @param {jQuery} Element to validate */
	this.validateCountryCodes = function(jObj) {
	  var content = jObj.val().trim().split(',');
	  var valid = true;
	  var error = '';

		content = YaCyUi.Tools.cleanStringArray(content);
	  if (content.length <= 0) {
	  	valid = false;
	  	var error = 'empty';
	  } else {
		  for (var i=0; i<content.length; i++) {
		  	if (!content[i].match(/^[a-z]{2}$/i)) {
		  		valid = false;
		  		var error = 'invalid-cc';
		  		break;
		  	}
		  }
	  }

	  if (valid) {
	  	state = {
	  		hints: {
	  			ok: true,
	  			clear: true,
	  			help: false,
	  			show: true
	  		},
	  		validation: {
	  			valid: true
	  		}
	  	};
	  } else {
	  	state = {
	  		hints: {
	  			error: error,
	  			help: true,
	  			show: true,
	  			clear: true
	  		},
	  		validation: {
	  			valid: false
	  		}
	  	};
	  }
		self.valid = state.validation.valid === true ? true : false;
		if (self.valid === false) {
			YaCyUi.Form.digOut(jObj.attr('id'));
		}
	  return state;
	};

  this.addInteractionHandler = function() {
  	YaCyPage.validable.push(self.e.countryMustMatchList); // store validable element
		YaCyUi.Form.Validate.addValidator(self.e.countryMustMatchList, {
	  	func: self.validateCountryCodes,
	  	delay: YaCyPage.validationDelay,
	  	onload: true
	  });
	  // field that must be non-empty
	  var validables = $('#mustmatch, #ipMustmatch');
	  validables.each(function() { // store validable element
	  	YaCyPage.validable.push($(this));
	  });
	  YaCyUi.Form.Validate.addValidator(validables, {
	  	func: new YaCyUi.Form.Validate.Validators.notEmpty({
	  		help: true,
	  		error: true
	  	}).validate,
	  	delay: YaCyPage.validationDelay,
	  	onload: true
	  });
	  // field that may be empty - but with warning
	  YaCyUi.Form.Validate.addValidator($('#indexmustmatch, #indexcontentmustmatch'), {
	  	func: new YaCyUi.Form.Validate.Validators.notEmpty({
	  		help: true,
	  		warning: true
	  	}, true).validate,
	  	delay: YaCyPage.validationDelay,
	  	onload: true
	  });
	};

	// init
  self.addInteractionHandler();
  return self;
};

YaCyPage.Report = {
	text: {
		editButton: 'Change',
		fixButton: 'Fix errors',
		changeDefaultsButton: 'Configure',
		confBookmarkTile: 'Bookmark title',
		useDefaultConfig: 'use default settings',
		emptyInput: 'left empty'
	},
	// cached elements
	e: null,
	visible: false,

	handeSpecialElements: function(e, id, section, data) {
		switch(id) {
			case 'reloadoldage':
			case 'deleteoldage':
				if (!e.is(':checked')) {
					return;
				}
				var text;
				if (id == 'deleteoldage') {
					text = 'If older than ' +
					$('#deleteIfOlderNumber').children('option:selected').text() +
					' ' + $('#deleteIfOlderUnit').children('option:selected').text();
				} else {
					text = 'If older than ' +
					$('#reloadIfOlderNumber').children('option:selected').text() +
					' ' + $('#reloadIfOlderUnit').children('option:selected').text();
				}

				var title = e.closest('fieldset').children('legend').text();
				data.content.append('<h1>' + title + '</h1>');
				data.entries.append('<dt class="noData">' + text + '</dt><dd>&nbsp;</dd>');
				break;
			case 'startPointSelectBox':
				if (YaCyPage.parts.startPoint.valid &&
						YaCyPage.CrawlStart.startType == 'single') {
					var label = section.find('label[for="' + id + '"]');
					data.entries.append('<dt>' + label.text() + '</dt><dd>'
					+ e.children('option:selected').text() + '</dd>');
				}
				break;
		}
	},

	parseSection: function(section, data) {
		section.find('input:not([type="button"]), select').not(':disabled')
				.not('[data-review="skip"]').each(function() {
			var id = this.id;
			var label = section.find('label[for="' + id + '"]');

			if ($(this).data('review') == 'custom') {
					YaCyPage.Report.handeSpecialElements($(this), id, section, data);
			} else if (this.tagName.toLowerCase() == 'select') {
				data.entries.append('<dt>' + label.text() + '</dt><dd>'
					+ $(this).children('option:selected').text() + '</dd>');
			} else if ($(this).attr('type').toLowerCase() == 'text'){
				var value = $(this).val().trim();
				// validable & valid?
				if (YaCyUi.Form.Validate.isValidable($(this)) &&
					!YaCyUi.Form.Validate.isValid($(this))) {
					data.isValid = false;
				}
				// content
				if (value.length === 0) {
					data.entries.append('<dt>' + label.text() +
						'</dt><dd class="empty">' + YaCyPage.Report.text.emptyInput + '</dd>');
				} else {
					data.entries.append('<dt>' + label.text() + '</dt><dd>'
						+ value + '</dd>');
				}
			} else if ($(this).is(':checked') === true) {
				data.entries.append('<dt class="noData">' + label.text()
					+ '</dt><dd>&nbsp;</dd>');
			}
		});
	},

	section: function(section) {
		var data = {
			content: $('<td>'),
			entries: $('<dl>'),
			isValid: true
		};
		var useDefaults = false;

		if (section.data('review') != 'skip-title') {
			data.content.append('<h1>' + section.children('legend').text() + '</h1>');
		}

		// handle special sections
		if (section.data('id') == 'startPoint') {
			if (YaCyPage.parts.startPoint.valid) {
				data.entries.append('<dt>' + YaCyPage.Report.text.confBookmarkTile +
					'</dt><dd>' + YaCyPage.CrawlStart.bookmarkTitle + '</dd>');
			} else {
				data.isValid = false;
			}
		} else if (section.is('.toggleable.ycu-toggle-hidden')) {
			data.entries.append('<dt class="notConfigured">' +
				YaCyPage.Report.text.useDefaultConfig + '</dt><dd>&nbsp;</dd>')
				.addClass('notConfigured');
			data.content.addClass('notConfigured');
			useDefaults = true;
		} else {
			YaCyPage.Report.parseSection(section, data);
			if (data.entries.children().size() === 0) {
				data.content = null;
			}
		}

		if (data.content !== null) {
			return {
				content: data.content.append(data.entries),
				defaults: useDefaults,
				valid: data.isValid
			};
		}
		return null;
	},

	group: function(content, group) {
		content.append('<tr class="group"><td colspan="2">'
			+ group.children('legend').text() + '</td></tr>');
			var editButton = $('<button class="icon-edit">' +
				YaCyPage.Report.text.editButton + '</button>');
			var fixButton = $('<button class="error icon-fix">' +
				YaCyPage.Report.text.fixButton + '</button>');
			var changeDefaultsButton = $('<button class="icon-config">'
				+ YaCyPage.Report.text.changeDefaultsButton + '</button>');

		group.children('fieldset.formSection').each(function() {
			$(this).uniqueId();
			var groupId = this.id;
			var sectionContent = YaCyPage.Report.section($(this));

			if (sectionContent !== null) {
				var button;

				if (sectionContent.defaults === true) {
					button = changeDefaultsButton.clone();
				} else if (sectionContent.valid === true) {
					button = editButton.clone();
				} else {
					button = fixButton.clone();
				}

				button.data('group-ref', groupId);

				content.append(
					$('<tr>')
					.append($('<td/>').html(button))
					.append(sectionContent.content)
				);
			}
		});
		return content;
	},

	generate: function() {
		var allValid = true;
		var report = $('<table><colgroup><col class="button"/><col class="content"/></colgroup><tbody></tbody></table>');
		var content = report.children('tbody');

		// cache elements
		if (YaCyPage.Report.e === null) {
			YaCyPage.Report.e = {
				review: $('#formReview'),
				formGroups: $('fieldset.formGroup'),
				mainForm: $('#crawler fieldset[data-id="main"]'),
				formControl: $('#crawler fieldset[data-id="formControl"]')
			};
		}

		report.on('click', 'button', function(evObj) {
			evObj.preventDefault();
			YaCyPage.Report.hide($(this).data('group-ref'));
		});

		YaCyPage.Report.e.review.children('.reviewContent').empty();

		// check if all parts are valid
		for (var part in YaCyPage.parts) {
			if (YaCyPage.parts[part].valid === false) {
				allValid = false;
				break;
			}
		}

		YaCyPage.Report.e.formGroups.each(function(){
			content = YaCyPage.Report.group(content, $(this));
		});


		YaCyPage.Report.e.review.children('.reviewContent').append(report);
		// show report
		YaCyPage.Report.show();
	},

	show: function(callback) {
		YaCyPage.Report.e.mainForm.children().not(YaCyPage.Report.e.formControl)
			.fadeOut('slow').promise().done(function() {
				YaCyPage.Report.e.formControl.find('button[data-id="check"]').hide();
				YaCyPage.Report.e.formControl.find('button[data-id="edit"]').show();
				YaCyPage.Report.e.review.fadeIn('slow').promise().done(function() {
					location.hash = "#" + YaCyPage.Report.e.review.attr('id');
					YaCyPage.Report.visible = true;
					if (typeof callback === 'function') {
						callback();
					}
				});
			});
	},

	hide: function(jumpTo, callback) {
		YaCyPage.Report.e.review.fadeOut('slow', function() {
			YaCyPage.Report.e.mainForm.children()
					.not(YaCyPage.Report.e.review).fadeIn('slow')
					.promise().done(function() {
				YaCyPage.Report.e.formControl.find('button[data-id="check"]').show();
				YaCyPage.Report.e.formControl.find('button[data-id="edit"]').hide();
				if (typeof jumpTo !== 'undefined' || jumpTo !== null) {
					YaCyUi.Form.digOut(jumpTo);
					location.hash = "#" + jumpTo;
				}
				YaCyPage.Report.visible = false;
				if (typeof callback === 'function') {
					callback();
				}
			});
		});
	},

	toggleDefaultsVisible: function() {
		var header = YaCyPage.Report.e.review.find('.reviewHeader');
		var content = YaCyPage.Report.e.review.find('.reviewContent');
		var state = header.data('defaultsVisible');
		var show;

		if (typeof state === 'undefined' || state === true) {
			header.find('button[data-id="showDefaults"]').show();
			header.find('button[data-id="hideDefaults"]').hide();
			header.data('defaultsVisible', false);
			show = false;
		} else {
			header.find('button[data-id="hideDefaults"]').show();
			header.find('button[data-id="showDefaults"]').hide();
			header.data('defaultsVisible', true);
			show = true;
		}

		if (show) {
			content.find('tr').show();
		} else {
			content.find('td.notConfigured').each(function() {
				var row = $(this).parents('tr');
				var head = row.prevUntil('tr.group').prev('tr').last();
				row.hide();
				if (head.nextUntil('tr.group').filter(':visible').size() == 0) {
					head.hide();
				}
			});
		}
	}
};

YaCyPage.Parts.FormControl = function() {
	var self = this;
	this.errorCount = 0;
	this.e = {
		btnSubmit: $('fieldset.formControl').find('button[type="submit"]')
	};

	/** Set error counter on submit button, based on validation states. */
	this.setSubmitState = function() {
		var errorHint = self.e.btnSubmit.children('span.errors');
		self.errorCount = 0;
		for (var i=0; i<YaCyPage.validable.length; i++) {
			if (!YaCyUi.Form.Validate.isValid(YaCyPage.validable[i])) {
				self.errorCount++;
			}
		};
		errorHint.empty();
		if (self.errorCount > 0) {
			errorHint.text('(' + self.errorCount + ' error' +
				(self.errorCount > 1 ? 's' : '') + ')');
		}
	};

	this.addInteractionHandler = function() {
		var formControl = $('fieldset.formControl');
		var btnCheck = formControl.find('button[data-id="check"]');

		$('#formReview').find('button[data-id$="Defaults"]')
				.on('click', function(evObj) {
			evObj.preventDefault();
			YaCyPage.Report.toggleDefaultsVisible();
		});

		btnCheck.prop('disabled', false).on('click', function(evObj) {
			evObj.preventDefault();
			// check if we need to load startPoint details first
			if (!YaCyPage.CrawlStart.dataLoaded && YaCyPage.parts.startPoint.valid) {
				self.e.btnSubmit.prop('disabled', true);
				YaCyUi.Form.Button.switchIcon(btnCheck, 'icon-loader');
				btnCheck.prop('disabled', true);
				YaCyPage.CrawlStart.getPagesInfo(function() {
					YaCyPage.parts.startPoint.pagesInfoLoaded();
					YaCyUi.Form.Button.switchIcon(btnCheck);
					btnCheck.prop('disabled', false);
					self.e.btnSubmit.prop('disabled', false);
					YaCyPage.Report.generate();
				}, self);
			} else {
				YaCyPage.Report.generate();
			}
		});

		formControl.find('button[data-id="edit"]').on('click', function(evObj) {
			evObj.preventDefault();
			YaCyPage.Report.hide('crawler');
		});

		self.e.btnSubmit.on('click', function(evObj) {
			evObj.preventDefault();
			if (self.errorCount > 0) {
				if (YaCyPage.Report.visible) {
					YaCyPage.Report.hide(null, function() {
						YaCyUi.Form.Validate.showResultsDialog(YaCyPage.validable);
					});
				} else {
					YaCyUi.Form.Validate.showResultsDialog(YaCyPage.validable);
				}
			} else {
				alert('ok - should submit now');
				YaCyUi.Form.Data.submit($('#crawler'));
			}
		});

		YaCyUi.Event.handle('validation-state', function(evObj, type, elements) {
			self.setSubmitState();
		});
	};

	// init
  self.addInteractionHandler();
};

/** Initialize the page. */
YaCyPage.init = function() {
	new YaCyPage.Parts.FormControl();
	YaCyPage.CrawlStart = new YaCyPage.Func.CrawlStart();
	// init parts and store validable ones
	YaCyPage.parts = {
		startPoint: new YaCyPage.Parts.StartPoint(),
		crawlerFilter: new YaCyPage.Parts.CrawlerFilter()
	}
};