 -- BV-217 ajout de la colonne Heure de clôture a l'export
 -- livre avec la version 1.1.9 du BO (juillet 2026)
 
 ALTER TABLE signalement_export add column heure_cloture varchar(5) DEFAULT '' NULL;
 
 CREATE OR REPLACE FUNCTION public.test_exp_ins_wkf()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare
 NewEtat varchar(255);
 NewIdMailServiceFait integer;
 NewDateCloture varchar (10) ;
 UserAccessCode varchar (255);
 IdLastHistory integer;
 NewExecuteurServiceFait varchar (255) :='';
 UserAdminEmail varchar (255);
 NewDerniereDateMAJ varchar (10);
 NewNombrePhotos integer;
 NewDateRequalification varchar(10) := '';
 NewHeureRequalification varchar(5) := '';
 NewDateEtatProgramme varchar(10) := '';
 NewHeureEtatProgramme varchar(5) := '';
 NameLastAction varchar (255) :='';
 NewHeureCloture varchar(5) := '';
begin
    -- état
	SELECT ws.name into NewEtat from workflow_state as ws where ws.id_state=new.id_state;

	-- date et heure de cloture
	if new.id_state=10
		THEN select to_char(service_fait_date_passage, 'DD/MM/YYYY'), to_char(NOW(), 'hh24:mi') into NewDateCloture, NewHeureCloture from signalement_signalement where new.id_resource=id_signalement ;
	elseif new.id_state=22
		THEN select to_char(date_mise_surveillance, 'DD/MM/YYYY'), to_char(NOW(), 'hh24:mi') into NewDateCloture, NewHeureCloture  from signalement_signalement where new.id_resource=id_signalement;
	elseif new.id_state=11
		THEN select to_char(date_rejet, 'DD/MM/YYYY'), to_char(NOW(), 'hh24:mi') into NewDateCloture, NewHeureCloture  from signalement_signalement where new.id_resource=id_signalement;
	else NewDateCloture  :='';
	end if;

    -- Récupérer les anciennes valeurs pour ne pas écraser
    SELECT COALESCE(date_requalification, ''),
           COALESCE(heure_requalification, ''),
           COALESCE(date_programme, ''),
           COALESCE(heure_programme, '')
    INTO NewDateRequalification, NewHeureRequalification, NewDateEtatProgramme, NewHeureEtatProgramme
    FROM signalement_export
    WHERE id_signalement = new.id_resource;


	-- date de requalification
	select wa.name into NameLastAction from workflow_resource_history wrh join workflow_action wa on wrh.id_action = wa.id_action where wrh.id_resource = new.id_resource order by wrh.id_history desc limit 1;

    if NameLastAction like '%Requalifier%' AND new.id_state IN ( select id_state from workflow_state where name like '%Transféré à un tiers%' )
		THEN
			select to_char(NOW(), 'DD/MM/YYYY'), to_char(NOW(), 'hh24:mi') into NewDateRequalification, NewHeureRequalification;
	end if;


	-- date etat programmé
	if new.id_state IN ( select distinct id_state from workflow_action wa join workflow_state ws on wa.id_state_after = ws.id_state where wa.name like '%Programmer%' )
		THEN
			select to_char(NOW(), 'DD/MM/YYYY'), to_char(NOW(), 'hh24:mi') into NewDateEtatProgramme, NewHeureEtatProgramme;
	end if;


	-- id mail service fait
	select val.id_message into NewIdMailServiceFait
	from signalement_workflow_notifuser_multi_contents_value as val
	inner join workflow_resource_history as history on history.id_history = val.id_history
	where history.id_resource=new.id_resource order by history.id_history desc limit 1;

	-- date de dernière prise en compte
	select id_history, to_char(creation_date, 'DD/MM/YYYY') into IdLastHistory, NewDerniereDateMAJ from workflow_resource_history where id_resource = new.id_resource order by creation_date desc limit 1;


	-- Exécuteur service fait (code user)
	select user_access_code into UserAccessCode from workflow_resource_history where id_resource = new.id_resource and id_action in (62,70,22,18,49,53,41);

	-- Mail exécuteur service fait
	select email into UserAdminEmail from  core_admin_user where access_code=UserAccessCode;

	-- Nom exécuteur service fait
	if UserAccessCode ='auto'
		then if (((select length(id_telephone) from  signalement_signaleur where fk_id_signalement = new.id_resource) > 0) and ( (select length(mail) from  signalement_signaleur where fk_id_signalement = new.id_resource) = 0 ))
				then NewExecuteurServiceFait := 'Mobil user';
			 else select mail into  NewExecuteurServiceFait from  signalement_signaleur where fk_id_signalement = new.id_resource;
			 end if;
	elseif length(UserAdminEmail)>0
		then NewExecuteurServiceFait := UserAdminEmail;

	end if;

     -- Photos
	select count (id_photo) into NewNombrePhotos from signalement_photo where fk_id_signalement = new.id_resource;

	if exists (select 1 from signalement_export where id_signalement=new.id_resource)
		then
			UPDATE signalement_export
			set id_wkf_state= new.id_state,etat=NewEtat, id_mail_service_fait=NewIdMailServiceFait, date_cloture=NewDateCloture, executeur_service_fait=NewExecuteurServiceFait, date_derniere_action=NewDerniereDateMAJ, nb_photos=NewNombrePhotos,
				date_requalification=NewDateRequalification, heure_requalification=NewHeureRequalification, date_programme=NewDateEtatProgramme, heure_programme=NewHeureEtatProgramme, heure_cloture=NewHeureCloture
			where id_signalement = new.id_resource;
	end if;

    RETURN NEW;
END;
$function$
;

-- rattrapage des donnees
UPDATE signalement_export se
SET heure_cloture = TO_CHAR(wrh.creation_date, 'hh24:mi')
FROM (
    SELECT id_resource,
           MAX(id_history) AS max_id_history
    FROM workflow_resource_history
    GROUP BY id_resource
) last_wrh
JOIN workflow_resource_history wrh
    ON wrh.id_history = last_wrh.max_id_history
WHERE se.id_signalement = wrh.id_resource
and se.etat in ('Service fait', 'Rejeté', 'Sous surveillance');