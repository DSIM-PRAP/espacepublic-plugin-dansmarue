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
package fr.paris.lutece.plugins.dansmarue.service.role;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.dansmarue.business.entities.Arrondissement;
import fr.paris.lutece.plugins.dansmarue.business.entities.FeuilleDeTournee;
import fr.paris.lutece.plugins.dansmarue.service.IArrondissementService;
import fr.paris.lutece.plugins.dansmarue.service.SignalementPlugin;
import fr.paris.lutece.plugins.dansmarue.utils.ListUtils;
import fr.paris.lutece.portal.service.rbac.Permission;
import fr.paris.lutece.portal.service.rbac.ResourceIdService;
import fr.paris.lutece.portal.service.rbac.ResourceType;
import fr.paris.lutece.portal.service.rbac.ResourceTypeManager;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.util.ReferenceList;

/**
 * FeuilleDeTournee resource id service.
 */
public class FeuilleDeTourneeResourceIdService extends ResourceIdService
{
    private static final String PROPERTY_LABEL_RESOURCE_TYPE = "dansmarue.rbac.feuille.de.tournee.resourceType.label";
    
    private static final String PROPERTY_LABEL_VIEW = "dansmarue.rbac.feuille.de.tournee.permission.view";
    private static final String PROPERTY_LABEL_CREATE = "dansmarue.rbac.feuille.de.tournee.permission.create";
    private static final String PROPERTY_LABEL_MODIFY = "dansmarue.rbac.feuille.de.tournee.permission.modify";
    private static final String PROPERTY_LABEL_DELETE = "dansmarue.rbac.feuille.de.tournee.permission.delete";
    private static final String PROPERTY_LABEL_EXPORT = "dansmarue.rbac.feuille.de.tournee.permission.export";
    private static final String PROPERTY_LABEL_TRANSFERT = "dansmarue.rbac.feuille.de.tournee.permission.transfert";
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void register( )
    {
        ResourceType rt = new ResourceType(  );
        rt.setResourceIdServiceClass( FeuilleDeTourneeResourceIdService.class.getName(  ) );
        rt.setPluginName( SignalementPlugin.PLUGIN_NAME );
        rt.setResourceTypeKey( FeuilleDeTournee.RESOURCE_TYPE );
        rt.setResourceTypeLabelKey( PROPERTY_LABEL_RESOURCE_TYPE );

        Permission p = new Permission(  );
        p.setPermissionKey( FeuilleDeTournee.PERMISSION_CREATE );
        p.setPermissionTitleKey( PROPERTY_LABEL_CREATE );
        rt.registerPermission( p );
        
        p = new Permission(  );
        p.setPermissionKey( FeuilleDeTournee.PERMISSION_MODIFY );
        p.setPermissionTitleKey( PROPERTY_LABEL_MODIFY );
        rt.registerPermission( p );
        
        p = new Permission(  );
        p.setPermissionKey( FeuilleDeTournee.PERMISSION_VIEW );
        p.setPermissionTitleKey( PROPERTY_LABEL_VIEW );
        rt.registerPermission( p );
        
        p = new Permission(  );
        p.setPermissionKey( FeuilleDeTournee.PERMISSION_DELETE );
        p.setPermissionTitleKey( PROPERTY_LABEL_DELETE );
        rt.registerPermission( p );
        
        p = new Permission(  );
        p.setPermissionKey( FeuilleDeTournee.PERMISSION_TRANSFERT );
        p.setPermissionTitleKey( PROPERTY_LABEL_TRANSFERT );
        rt.registerPermission( p );
        
        p = new Permission(  );
        p.setPermissionKey( FeuilleDeTournee.PERMISSION_EXPORT );
        p.setPermissionTitleKey( PROPERTY_LABEL_EXPORT );
        rt.registerPermission( p );
        
        ResourceTypeManager.registerResourceType( rt );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList getResourceIdList( Locale locale )
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle( String strId, Locale locale )
    {
        return null;
    }

}
