/*
 * Copyright (c) 2002-2021, City of Paris
 * All rights reserved.
 * License 1.0
 */
package fr.paris.lutece.plugins.dansmarue.utils;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper JSON basé sur Jackson (fasterxml), introduit lors de la montée lutece-core 6.1.0 → 7.0.6
 * pour remplacer la dépendance {@code net.sf.json} (json-lib) retirée du core en v7.
 *
 * <p>
 * Il centralise la construction / le parsing JSON afin de maîtriser en UN seul endroit le rendu des
 * réponses de web services (consommées notamment par les applications mobiles DansMaRue).
 * </p>
 *
 * <p>
 * <b>LUTECE-UPGRADE-SIGNAL</b> : la sérialisation des valeurs (notamment les {@link java.util.Date})
 * peut différer de l'ancienne lib {@code net.sf.json}. La <b>parité exacte des payloads WS</b> doit
 * être validée AVANT/APRÈS montée (Phase 3). Point d'ajustement unique : {@link #accumulate}.
 * </p>
 */
public final class DmrJson
{
    /** ObjectMapper partagé (thread-safe). */
    private static final ObjectMapper MAPPER = new ObjectMapper( );

    private DmrJson( )
    {
    }

    /**
     * @return le mapper partagé
     */
    public static ObjectMapper mapper( )
    {
        return MAPPER;
    }

    /**
     * @return un nouvel objet JSON mutable (remplace {@code new net.sf.json.JSONObject()})
     */
    public static ObjectNode object( )
    {
        return MAPPER.createObjectNode( );
    }

    /**
     * @return un nouveau tableau JSON mutable (remplace {@code new net.sf.json.JSONArray()})
     */
    public static ArrayNode array( )
    {
        return MAPPER.createArrayNode( );
    }

    /**
     * Ajoute une valeur sous une clé (équivalent de {@code JSONObject.accumulate} pour un ajout simple :
     * dans le code DansMaRue chaque clé n'est écrite qu'une fois, la sémantique est donc celle d'un put).
     *
     * @param obj
     *            l'objet cible
     * @param key
     *            la clé
     * @param value
     *            la valeur (JsonNode, String, Number, Boolean, bean… ; {@code null} → null JSON)
     */
    public static void accumulate( ObjectNode obj, String key, Object value )
    {
        if ( value == null )
        {
            obj.putNull( key );
        }
        else if ( value instanceof JsonNode )
        {
            obj.set( key, (JsonNode) value );
        }
        else if ( value instanceof String )
        {
            obj.put( key, (String) value );
        }
        else if ( value instanceof Integer )
        {
            obj.put( key, (Integer) value );
        }
        else if ( value instanceof Long )
        {
            obj.put( key, (Long) value );
        }
        else if ( value instanceof Double )
        {
            obj.put( key, (Double) value );
        }
        else if ( value instanceof Float )
        {
            obj.put( key, (Float) value );
        }
        else if ( value instanceof Boolean )
        {
            obj.put( key, (Boolean) value );
        }
        else
        {
            // beans, Date, collections : convertir IMMÉDIATEMENT en arbre JSON via le mapper.
            // putPOJO crée un POJONode qui n'est sérialisé correctement que si l'écriture passe par le
            // mapper Jackson ; or le code écrit souvent la réponse via result.toString() (JsonNode.toString,
            // SANS contexte mapper) → les POJO retombent sur leur toString() Java (ex. "Sector@hash" →
            // JSON invalide, AJAX cassé). valueToTree garantit un JSON valide quel que soit le mode d'écriture.
            obj.set( key, MAPPER.valueToTree( value ) );
        }
    }

    /**
     * Convertit un bean / une collection en arbre JSON (remplace {@code JSONObject/JSONArray.fromObject(bean)}).
     *
     * @param bean
     *            l'objet
     * @return l'arbre JSON
     */
    public static JsonNode fromObject( Object bean )
    {
        return MAPPER.valueToTree( bean );
    }

    /**
     * Parse une chaîne en objet JSON (remplace {@code JSONObject.fromObject(String)}).
     *
     * @param json
     *            la chaîne
     * @return l'objet
     * @throws JsonProcessingException
     *             si la chaîne n'est pas un objet JSON valide
     */
    public static ObjectNode parseObject( String json ) throws JsonProcessingException
    {
        JsonNode node = MAPPER.readTree( json );
        if ( !( node instanceof ObjectNode ) )
        {
            throw new IllegalArgumentException( "JSON attendu de type objet" );
        }
        return (ObjectNode) node;
    }

    /**
     * Parse une chaîne en tableau JSON (remplace {@code JSONArray.fromObject(String)}).
     *
     * @param json
     *            la chaîne
     * @return le tableau
     * @throws JsonProcessingException
     *             si la chaîne n'est pas un tableau JSON valide
     */
    public static ArrayNode parseArray( String json ) throws JsonProcessingException
    {
        JsonNode node = MAPPER.readTree( json );
        if ( !( node instanceof ArrayNode ) )
        {
            throw new IllegalArgumentException( "JSON attendu de type tableau" );
        }
        return (ArrayNode) node;
    }

    /**
     * Convertit un tableau JSON en liste de chaînes (remplace un usage courant de {@code JSONArray.toCollection}).
     *
     * @param array
     *            le tableau
     * @return la liste des valeurs texte
     */
    public static List<String> toStringList( ArrayNode array )
    {
        List<String> list = new ArrayList<>( );
        if ( array != null )
        {
            array.forEach( n -> list.add( n.asText( ) ) );
        }
        return list;
    }

    /**
     * Convertit un tableau JSON en liste d'entiers (remplace {@code JSONArray.toCollection} quand les éléments
     * sont des entiers, cf. filtres feuille de tournée).
     *
     * @param array
     *            le tableau
     * @return la liste des entiers
     */
    public static List<Integer> toIntegerList( ArrayNode array )
    {
        List<Integer> list = new ArrayList<>( );
        if ( array != null )
        {
            array.forEach( n -> list.add( n.asInt( ) ) );
        }
        return list;
    }
}
