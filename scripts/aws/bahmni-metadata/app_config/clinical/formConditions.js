Bahmni.ConceptSet.FormConditions.rules = {
  'RMRCT, Previsiosly treated with TB' : function (formName, formFieldValues) {
	var conditions = {
		enable : [],
		disable : []
	};
	var previousTreatmentWhere = "RMRCT, If treatment taken, Where was the patient treated ?";
	var previousTreamentWhen = "RMRCT, If treatment taken, When was the patient treated ?";
	var previousTreatmentCount = "RMRCT, If treatment taken, How many times was the patient treated ?";
	var previousTreatmentDots = "RMRCT, If treatment taken, If Dots taken : Category";
	var previousTreatmentDrugs = "CDST Form, Drugs used in his treatment";
	var result = formFieldValues['RMRCT, Previsiosly treated with TB'];
	if (result == "Yes") {
		conditions.enable.push(previousTreatmentWhere,previousTreamentWhen,previousTreatmentCount,previousTreatmentDots,previousTreatmentDrugs);
	} else {
		conditions.disable.push(previousTreatmentWhere,previousTreamentWhen,previousTreatmentCount,previousTreatmentDots,previousTreatmentDrugs);
	}
	return conditions;
  },
	'Recent Diagnosis': function (formName, formFieldValues, patient) {
		var recentDiagnosis = formFieldValues['Recent Diagnosis'];
		var currentLocation = window.location.origin;
		var pathName = window.location.pathname;
		var patientUuid = patient.uuid;
		var diagnosisRedirectUrl = `${currentLocation}/${pathName}#/default/patient/${patientUuid}/dashboard/diagnosis`;
		if (recentDiagnosis === 'Fill Now') {
			window.open(diagnosisRedirectUrl, '_blank');
			var noButton = document.querySelector('button[title="Fill Now"]');
			if (noButton) {
				setTimeout(function () {
					noButton.click();
				});
			}
		}
	},

	'Habits': function (formName, formFieldValues, patient) {
		var conditions = { show: [], hide: [] }
		var templateHabitsValues = formFieldValues['Habits'];

		if (templateHabitsValues.length === 0) {
			conditions.hide.push("Gudakhu Habit If Yes", "Alcohol Habit If Yes", "Smoking Habit If Yes", "Tobacco Habit If Yes", "Gutkha Habit If Yes", "Ganja Habit If Yes");
		} else {
			if (templateHabitsValues.includes('Gudakhu')) {
				conditions.show.push("Gudakhu Habit If Yes");
			} else {
				conditions.hide.push("Gudakhu Habit If Yes");
			}
			if (templateHabitsValues.includes('Alcohol')) {
				conditions.show.push("Alcohol Habit If Yes");
			} else {
				conditions.hide.push("Alcohol Habit If Yes");
			}
			if (templateHabitsValues.includes('Smoking')) {
				conditions.show.push("Smoking Habit If Yes");
			} else {
				conditions.hide.push("Smoking Habit If Yes");
			}
			if (templateHabitsValues.includes('Tobacco')) {
				conditions.show.push("Tobacco Habit If Yes");
			} else {
				conditions.hide.push("Tobacco Habit If Yes");
			}
			if (templateHabitsValues.includes('Gutkha')) {
				conditions.show.push("Gutkha Habit If Yes");
			} else {
				conditions.hide.push("Gutkha Habit If Yes");
			}
			if (templateHabitsValues.includes('Ganja')) {
				conditions.show.push("Ganja Habit If Yes");
			} else {
				conditions.hide.push("Ganja Habit If Yes");
			}
		}
		return conditions;
	}
};