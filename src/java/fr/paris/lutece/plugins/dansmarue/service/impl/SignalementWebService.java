/*
 * Copyright (c) 2002-2021, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.dansmarue.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.dansmarue.business.dao.IAdresseDAO;
import fr.paris.lutece.plugins.dansmarue.business.entities.PhotoDMR;
import fr.paris.lutece.plugins.dansmarue.business.entities.Signalement;
import fr.paris.lutece.plugins.dansmarue.commons.exceptions.BusinessException;
import fr.paris.lutece.plugins.dansmarue.commons.exceptions.TechnicalException;
import fr.paris.lutece.plugins.dansmarue.service.ISignalementWebService;
import fr.paris.lutece.plugins.dansmarue.utils.DateUtils;
import fr.paris.lutece.plugins.dansmarue.utils.SignalementUtils;
import fr.paris.lutece.plugins.dansmarue.utils.ws.IWebServiceCaller;
import fr.paris.lutece.portal.service.image.ImageResource;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.httpaccess.HttpAccessException;
import fr.paris.lutece.util.signrequest.RequestAuthenticator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.paris.lutece.plugins.dansmarue.utils.DmrJson;

/**
 * The Class SignalementWebService.
 */
public class SignalementWebService implements ISignalementWebService
{

    /** The Constant REQUEST_METHOD_ADD. */
    // JSON TAG
    public static final String REQUEST_METHOD_ADD = "addAnomalie";

    /** The Constant REQUEST_METHOD_DONE. */
    public static final String REQUEST_METHOD_DONE = "serviceDoneAnomalie";

    /** The Constant JSON_TAG_ANOMALIE. */
    public static final String JSON_TAG_ANOMALIE = "anomalie";

    /** The Constant JSON_TAG_UDID. */
    public static final String JSON_TAG_UDID = "udid";

    /** The Constant JSON_TAG_EMAIL. */
    public static final String JSON_TAG_EMAIL = "email";

    /** The Constant JSON_TAG_PHOTOS. */
    public static final String JSON_TAG_PHOTOS = "photos";

    /** The Constant TAG_REQUEST. */
    private static final String TAG_REQUEST = "request";

    /** The Constant TAG_ERROR. */
    private static final String TAG_ERROR = "error";

    /** The Constant TAG_ANSWER. */
    private static final String TAG_ANSWER = "answer";

    /** The ws caller. */
    @Inject
    private IWebServiceCaller _wsCaller;

    /** The authenticator. */
    @Inject
    @Named( "rest.requestAuthenticator" )
    private RequestAuthenticator _authenticator;

    /** The adresse signalement DAO. */
    @Inject
    @Named( "signalementAdresseDAO" )
    private IAdresseDAO _adresseSignalementDAO;

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectNode getJSONResponse( Signalement signalement, String url )
    {
        ObjectNode response = null;

        if ( signalement.getAdresses( ).isEmpty( ) || !SignalementUtils.isValidAddress( signalement.getAdresses( ).get( 0 ).getAdresse( ) ) )
        {
            return generateErrorResponse( "Error invalid address id signalement " + signalement.getId( ) );
        }

        try
        {
            String strResp = sendByWS( signalement, url );
            ArrayNode array = DmrJson.parseArray( strResp );
            response = (ObjectNode) array.get( 0 );
        }
        catch( BusinessException e )
        {
            AppLogService.error( e.getMessage( ), e );
            response = generateErrorResponse( "Error when contacting " + url );
        }
        catch( UnsupportedEncodingException e )
        {
            AppLogService.error( e.getMessage( ), e );
            response = generateErrorResponse( "Encoding error" );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( e.getMessage( ), e );
            response = generateErrorResponse( "Get response error" );
        }
        catch( Exception e )
        {
            AppLogService.error( e.getMessage( ), e );
            response = generateErrorResponse( "Unexpected error" );
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String sendByWS( Signalement signalement, String url ) throws UnsupportedEncodingException
    {
        String result = null;

        if ( signalement == null )
        {
            throw new BusinessException( signalement, "dansmarue.ws.error.url.empty" );
        }

        ObjectNode json = createJSON( signalement );

        ObjectNode jsonSrc = DmrJson.object( );
        DmrJson.accumulate(jsonSrc, JSON_TAG_ANOMALIE, json );

        // name of the method in REST api
        DmrJson.accumulate(jsonSrc, TAG_REQUEST, REQUEST_METHOD_ADD );

        Map<String, List<String>> params = new HashMap<>( );
        List<String> values = new ArrayList<>( );

        String jsonFormated = jsonSrc.toString( );
        values.add( jsonFormated );
        params.put( "jsonStream", values );

        try
        {
            AppLogService.info( "Call web service " + url + " for id anomalie : " + signalement.getId( ) );
            // Suppression des photos pour ne pas surcharger les logs
            ( (ObjectNode) jsonSrc.get( JSON_TAG_ANOMALIE ) ).remove( JSON_TAG_PHOTOS );
            AppLogService.info( "Flux Json : " + jsonSrc.toString( ) );
            result = _wsCaller.callWebService( url, params, _authenticator, values );
        }
        catch( Exception e )
        {
            AppLogService.error( e.getMessage( ), e );
            throw new BusinessException( signalement, "dansmarue.ws.error.url.connexion" );
        }

        AppLogService.info( "Web service response for id anomalie : " + signalement.getId( ) + " is : " + result );
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectNode createJSON( Signalement signalement ) throws UnsupportedEncodingException
    {
        // content of the json stream must be encode, because using application/x-www-form-urlencoded

        ObjectNode jsonAnomalie = DmrJson.object( );
        DmrJson.accumulate(jsonAnomalie, "id", signalement.getId( ) );
        DmrJson.accumulate(jsonAnomalie, "reference", signalement.getNumeroSignalement( ) );
        DmrJson.accumulate(jsonAnomalie, "date_creation", signalement.getDateCreation( ) );
        DmrJson.accumulate(jsonAnomalie, "heure_creation", DateUtils.getHourWithSecondsFr( signalement.getHeureCreation( ) ) );
        DmrJson.accumulate(jsonAnomalie, "commentaire", encode( signalement.getCommentaire( ) ) );
        DmrJson.accumulate(jsonAnomalie, "type", encode( signalement.getType()));
        DmrJson.accumulate(jsonAnomalie, "priorite", encode( signalement.getPriorite( ).getLibelle( ) ) );
        DmrJson.accumulate(jsonAnomalie, "adresse", encode( signalement.getAdresses( ).get( 0 ).getAdresse( ) ) );
        DmrJson.accumulate(jsonAnomalie, "lat", signalement.getAdresses( ).get( 0 ).getLat( ) );
        DmrJson.accumulate(jsonAnomalie, "lng", signalement.getAdresses( ).get( 0 ).getLng( ) );
        DmrJson.accumulate(jsonAnomalie, "token", signalement.getToken( ) );

        List<PhotoDMR> photos = signalement.getPhotos( );

        ArrayNode array = DmrJson.array( );
        if ( ( photos != null ) && !photos.isEmpty( ) )
        {
            for ( PhotoDMR p : photos )
            {
                ObjectNode photoJson = DmrJson.object( );
                DmrJson.accumulate(photoJson, "id_photo", p.getId( ) );
                DmrJson.accumulate(photoJson, "vue_photo", p.getVue( ) );
                DmrJson.accumulate(photoJson, "photo", getImageBase64( p.getImage( ) ) );
                array.add( photoJson );
            }
            DmrJson.accumulate(jsonAnomalie, JSON_TAG_PHOTOS, array );
        }
        else
        {
            DmrJson.accumulate(jsonAnomalie, JSON_TAG_PHOTOS, array );
        }

        return jsonAnomalie;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectNode callWSPartnerServiceDone( Signalement signalement, String urlPartner )
    {

        ObjectNode response = null;
        String result = null;

        Map<String, List<String>> params = new HashMap<>( );
        List<String> values = new ArrayList<>( );

        ObjectNode jsonSrc = DmrJson.object( );
        DmrJson.accumulate(jsonSrc, TAG_REQUEST, REQUEST_METHOD_DONE );
        DmrJson.accumulate(jsonSrc, "id", signalement.getId( ) );
        DmrJson.accumulate(jsonSrc, "reference", signalement.getNumeroSignalement( ) );
        DmrJson.accumulate(jsonSrc, "token", signalement.getToken( ) );
        DmrJson.accumulate(jsonSrc, "date_creation", signalement.getDateCreation( ) );
        DmrJson.accumulate(jsonSrc, "date_service_fait", signalement.getDateServiceFaitTraitement( ) );

        String jsonFormated = jsonSrc.toString( );
        values.add( jsonFormated );
        params.put( "jsonStream", values );

        try
        {
            AppLogService.info( "Call web service PartnerServiceDone " + urlPartner + " for id anomalie : " + signalement.getId( ) );
            AppLogService.info( "Flux Json : " + jsonFormated );
            result = _wsCaller.callWebService( urlPartner, params, _authenticator, values );
        }
        catch( HttpAccessException e )
        {
            AppLogService.error( e.getMessage( ), e );
            throw new BusinessException( signalement.getId( ), "dansmarue.ws.error.url.connexion" );
        }

        try
        {
            ArrayNode array = DmrJson.parseArray( result );
            response = (ObjectNode) array.get( 0 );
        }
        catch( JsonProcessingException e1 )
        {
            try
            {
                AppLogService.info( "Received ObjectNode is not of the regular type (ObjectNode in ArrayNode)" );
                response = DmrJson.parseObject( result );
            }
            catch( JsonProcessingException e2 )
            {
                AppLogService.info( "Received ObjectNode is not of the irregular type (ObjectNode)" );
                AppLogService.error( e2.getMessage( ), e2 );
                throw new TechnicalException( e2.getMessage( ), e2.getCause( ) );
            }
        }

        AppLogService.info( "Web service PartnerServiceDone response for id anomalie : " + signalement.getId( ) + " is : " + result );
        return response;
    }

    /**
     * Encode report data before create JSON.
     *
     * @param data
     *            the signalement data
     * @return the encoded data
     * @throws UnsupportedEncodingException
     *             when charset cannot be use
     */
    private String encode( String data ) throws UnsupportedEncodingException
    {
        return StringUtils.isNotBlank( data ) ? URLEncoder.encode( data, CharEncoding.UTF_8 ) : StringUtils.EMPTY;
    }

    /**
     * Encode Image for WebService transport.
     *
     * @param image
     *            the image object
     * @return Encode Image
     */
    private String getImageBase64( ImageResource image )
    {
        String dataImg = "";
        if ( ( image != null ) && ( image.getImage( ) != null ) )
        {
            Base64 codec = new Base64( );
            String data = new String( codec.encode( image.getImage( ) ) );
            String mimeType = ( image.getMimeType( ) == null ) ? "data:image/jpg;base64," : ( "data:" + image.getMimeType( ) + ";base64," );
            dataImg = mimeType + data;
        }

        return dataImg;
    }

    /**
     * Generate Error response.
     *
     * @param message
     *            the message
     * @return Json error response
     */
    private ObjectNode generateErrorResponse( String message )
    {

        ObjectNode response = DmrJson.object( );
        DmrJson.accumulate(response, TAG_REQUEST, REQUEST_METHOD_ADD );
        ObjectNode error = DmrJson.object( );
        DmrJson.accumulate(error, TAG_ERROR, message );
        DmrJson.accumulate(response, TAG_ANSWER, error );

        return response;
    }

}
