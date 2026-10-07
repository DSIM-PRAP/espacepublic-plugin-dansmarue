-- BV-250 suppression colonne precision_localisation
ALTER TABLE signalement_adresse DROP COLUMN IF EXISTS precision_localisation;

--BV-250 nettoyer les notification contenant la precision location
UPDATE signalement_workflow_notification_config SET message = replace(message, '$' || '{precision}', '') WHERE message LIKE '%$' || '{precision}%';
UPDATE signalement_workflow_notification_service_programme_config SET message = replace(message, '$' || '{precision}', '') WHERE message LIKE '%$' || '{precision}%';
UPDATE signalement_workflow_notification_suivi_config SET mail_message = replace(mail_message, '$' || '{precision}', '') WHERE mail_message LIKE '%$' || '{precision}%';
UPDATE signalement_workflow_notification_user_config SET message = replace(message, '$' || '{precision}', '') WHERE message LIKE '%$' || '{precision}%';
UPDATE signalement_workflow_notifuser_multi_contents_config SET message = replace(message, '$' || '{precision}', '') WHERE message LIKE '%$' || '{precision}%';