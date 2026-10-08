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
package fr.paris.lutece.plugins.dansmarue.business.dao.impl;

import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.dansmarue.business.dao.IAdresseDAO;
import fr.paris.lutece.plugins.dansmarue.business.entities.Adresse;
import fr.paris.lutece.plugins.dansmarue.business.entities.Signalement;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * The Class AdresseDAO.
 */
public class AdresseDAO implements IAdresseDAO
{

    /** The Constant SQL_QUERY_NEW_PK. */
    private static final String SQL_QUERY_NEW_PK = " SELECT nextval('seq_signalement_adresse_id_adresse')";

    /** The Constant SQL_QUERY_INSERT_WITH_GEOM. */
    private static final String SQL_QUERY_INSERT_WITH_GEOM = " INSERT INTO signalement_adresse(id_adresse, adresse, fk_id_signalement, geom) VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))";

    /** The Constant SQL_QUERY_DELETE. */
    private static final String SQL_QUERY_DELETE = " DELETE FROM signalement_adresse WHERE id_adresse = ? ";

    /** The Constant SQL_QUERY_SELECT. */
    private static final String SQL_QUERY_SELECT = " SELECT id_adresse, adresse, ST_X(geom), ST_Y(geom), fk_id_signalement FROM signalement_adresse WHERE id_adresse = ? ";

    /** The Constant SQL_QUERY_SELECT_BY_SIGNALEMENT. */
    private static final String SQL_QUERY_SELECT_BY_SIGNALEMENT = " SELECT id_adresse, adresse, ST_X(geom), ST_Y(geom), fk_id_signalement FROM signalement_adresse WHERE fk_id_signalement = ? ";

    /** The Constant SQL_QUERY_UPDATE. */
    private static final String SQL_QUERY_UPDATE = " UPDATE signalement_adresse SET id_adresse=?, adresse=?, geom=ST_SetSRID(ST_MakePoint(?, ?), 4326), fk_id_signalement=? WHERE id_adresse = ? ";

    /** The Constant SQL_QUERY_UPDATE_ADRESSE. */
    private static final String SQL_QUERY_UPDATE_ADRESSE = " UPDATE signalement_adresse SET adresse=?, is_adresse_rattrapee=TRUE WHERE id_adresse = ? ";

    /** The Constant SQL_QUERY_SELECT_WRONG_ADRESSES. */
    private static final String SQL_QUERY_SELECT_WRONG_ADRESSES = "select id_adresse, adresse, ST_X(geom), ST_Y(geom), is_adresse_rattrapee ";
    private static final String SQL_QUERY_WRONG_TOWN_SAINT_DENIS ="from (select * from signalement_adresse  where (adresse not similar to '%93[0-9]{3} Saint-Denis') ";
    private static final String SQL_QUERY_WRONG_TOWN_AUBERVILLIERS ="intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} Aubervilliers') ";
    private static final String SQL_QUERY_WRONG_TOWN_EPINAY_SUR_SAINE="intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} Épinay-sur-Seine') ";
    private static final String SQL_QUERY_WRONG_TOWN_COURNEUVE="intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} La Courneuve') ";
    private static final String SQL_QUERY_WRONG_TOWN_ILE_SAINT_DENIS="intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} L''Île-Saint-Denis') ";
    private static final String SQL_QUERY_WRONG_TOWN_PIERREFITTE_SUR_SEINE="intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} Pierrefitte-sur-Seine') ";
    private static final String SQL_QUERY_WRONG_TOWN_SAINT_OUEN_SUR_SEINE="intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} Saint-Ouen-sur-Seine') ";
    private static final String SQL_QUERY_WRONG_TOWN_STAINS = "intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} Stains') ";
    private static final String SQL_QUERY_WRONG_TOWN_VILLETANEUSE = "intersect select * from signalement_adresse where (adresse not similar to '%93[0-9]{3} Villetaneuse')) as subrequest ";
    private static final String SQL_QUERY_WHERE_ORDER_WRONG_TOWN_= "where is_adresse_rattrapee is false order by id_adresse desc limit 5;";

    /** The Constant SQL_QUERY_WRONG_ADRESSES_STREET. */
    private static final String SQL_QUERY_WRONG_ADRESSES_STREET= "select id_adresse, adresse, ST_X(geom), ST_Y(geom), is_adresse_rattrapee from signalement_adresse"
            + " where (replace((regexp_split_to_array(adresse, '93[0-9]{3}'))[1],' ','') similar to  '%[0-9]' or (regexp_split_to_array(adresse, '93[0-9]{3}'))[1].length <= 1)"
            + " and is_adresse_rattrapee is false order by id_adresse desc limit 5;";

    /** The Constant SQL_QUERY_SELECT_COORDONATE_WSG84. */
    private static final String SQL_QUERY_SELECT_COORDONATE_WSG84 = "select ST_X(geom), ST_Y(geom) from signalement_adresse where id_adresse=?";

    /** The Constant SQL_QUERY_FIX_VIRGULE_CP. */
    private static final String SQL_QUERY_FIX_VIRGULE_CP = " update signalement_adresse set adresse = regexp_replace(adresse, SUBSTRING(adresse, ' 75[0-9]{3}'), ', '||SUBSTRING(adresse, '75[0-9]{3}')) where adresse not similar to '%, 75[0-9]{3}%' and adresse not similar to '%,75[0-9]{3}%' and adresse similar to '%75[0-9]{3}%' ";

    /** The Constant SQL_QUERY_FIX_VILLE. */
    private static final String SQL_QUERY_FIX_VILLE = " update signalement_adresse sa " + "set adresse = subquerry.newadresse " + "from ( "
            + " select sa1.id_adresse, newadresse " + " from signalement_adresse sa1 " + " left join ( "
            + "     select *, adresse, position('Parigi' in adresse), replace(substring(adresse from 0 for position('Parigi' in adresse) +6), 'Parigi','PARIS') as newadresse "
            + "	 from signalement_adresse  " + "	 where adresse ~* 'Parigi' and position('Parigi' in adresse) > 6 " + "  ) as sa2 "
            + " on sa1.id_adresse = sa2.id_adresse " + " where sa2.newadresse is not null " + " ) as subquerry "
            + " where sa.id_adresse = subquerry.id_adresse " + " and adresse ~* 'Parigi' and position('Parigi' in adresse) > 6 ";

    /** The Constant SQL_QUERY_FIX_SYNTAXE_ARRONDISSEMENT. */
    private static final String SQL_QUERY_FIX_SYNTAXE_ARRONDISSEMENT = "update signalement_adresse sa" + " set adresse = subquerry.newadresse" + " from ("
            + " select sa1.id_adresse, newadresse" + " from signalement_adresse sa1" + " left join ("
            + "     select *, adresse, position('Paris' in adresse), replace(substring(adresse from 0 for position('Paris' in adresse) +5), 'Paris','PARIS') as newadresse"
            + "	 from signalement_adresse " + "	 where adresse ~* 'Arrondissement' and position('Paris' in adresse) > 5" + "  ) as sa2"
            + " on sa1.id_adresse = sa2.id_adresse" + " where sa2.newadresse is not null" + " ) as subquerry" + " where sa.id_adresse = subquerry.id_adresse"
            + " and adresse ~* 'Arrondissement' and position('Paris' in adresse) > 5";

    /**
     * Generates a new primary key.
     *
     * @return The new primary key
     */
    private Long newPrimaryKey( )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_NEW_PK );
        daoUtil.executeQuery( );

        Long nKey = null;

        if ( daoUtil.next( ) )
        {
            nKey = daoUtil.getLong( 1 );
        }

        daoUtil.close( );

        return nKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update( Adresse adresse )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE );
        int nIndex = 1;
        daoUtil.setLong( nIndex++, adresse.getId( ) );
        daoUtil.setString( nIndex++, adresse.getAdresse( ) );
        daoUtil.setDouble( nIndex++, adresse.getLng( ) );
        daoUtil.setDouble( nIndex++, adresse.getLat( ) );
        daoUtil.setLong( nIndex++, adresse.getSignalement( ).getId( ) );

        daoUtil.setLong( nIndex, adresse.getId( ) );

        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAdresse( Adresse adresse )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE_ADRESSE );
        int nIndex = 1;
        daoUtil.setString( nIndex++, adresse.getAdresse( ) );
        daoUtil.setLong( nIndex, adresse.getId( ) );

        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Long insert( Adresse adresse )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT_WITH_GEOM ) )
        {

            if ( ( adresse.getId( ) == null ) || ( adresse.getId( ) == 0 ) )
            {
                adresse.setId( newPrimaryKey( ) );

                int nIndex = 1;
                daoUtil.setLong( nIndex++, adresse.getId( ) );
                daoUtil.setString( nIndex++, adresse.getAdresse( ) );
                daoUtil.setLong( nIndex++, adresse.getSignalement( ).getId( ) );
                daoUtil.setDouble( nIndex++, adresse.getLng( ) );
                daoUtil.setDouble( nIndex, adresse.getLat( ) );

                daoUtil.executeUpdate( );
            }

            return adresse.getId( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove( long lId )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE );
        daoUtil.setLong( 1, lId );
        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Adresse load( long lId )
    {
        Adresse adresse = new Adresse( );
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT );
        daoUtil.setLong( 1, lId );
        daoUtil.executeQuery( );

        if ( daoUtil.next( ) )
        {
            int nIndex = 1;
            adresse.setId( daoUtil.getLong( nIndex++ ) );
            adresse.setAdresse( daoUtil.getString( nIndex++ ) );

            adresse.setLng( daoUtil.getDouble( nIndex++ ) );
            adresse.setLat( daoUtil.getDouble( nIndex++ ) );

            Signalement signalement = new Signalement( );
            signalement.setId( daoUtil.getLong( nIndex ) );
            adresse.setSignalement( signalement );
        }

        daoUtil.close( );

        return adresse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Adresse loadByIdSignalement( long lId )
    {
        Adresse adresse = new Adresse( );
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_SIGNALEMENT );
        daoUtil.setLong( 1, lId );
        daoUtil.executeQuery( );

        if ( daoUtil.next( ) )
        {
            int nIndex = 1;
            adresse.setId( daoUtil.getLong( nIndex++ ) );
            adresse.setAdresse( daoUtil.getString( nIndex++ ) );

            adresse.setLng( daoUtil.getDouble( nIndex++ ) );
            adresse.setLat( daoUtil.getDouble( nIndex++ ) );

            Signalement signalement = new Signalement( );
            signalement.setId( daoUtil.getLong( nIndex ) );

            adresse.setSignalement( signalement );
        }

        daoUtil.close( );

        return adresse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( Adresse adresse )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE );
        int nIndex = 1;
        daoUtil.setLong( nIndex++, adresse.getId( ) );
        daoUtil.setString( nIndex++, adresse.getAdresse( ) );
        daoUtil.setLong( nIndex++, adresse.getSignalement( ).getId( ) );
        // WHERE
        daoUtil.setLong( nIndex, adresse.getId( ) );

        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Adresse> findBySignalementId( long lIdSignalement )
    {
        List<Adresse> result = new ArrayList<>( );

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_SIGNALEMENT );
        daoUtil.setLong( 1, lIdSignalement );

        daoUtil.executeQuery( );

        // Pour chaque resultat retourne
        while ( daoUtil.next( ) )
        {
            Adresse adresse = new Adresse( );
            int nIndex = 1;
            adresse.setId( daoUtil.getLong( nIndex++ ) );
            adresse.setAdresse( daoUtil.getString( nIndex++ ) );

            adresse.setLng( daoUtil.getDouble( nIndex++ ) );
            adresse.setLat( daoUtil.getDouble( nIndex++ ) );

            Signalement signalement = new Signalement( );
            signalement.setId( daoUtil.getLong( nIndex ) );
            adresse.setSignalement( signalement );
            result.add( adresse );
        }

        daoUtil.close( );

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Adresse> findWrongAdresses( )
    {
        List<Adresse> result = new ArrayList<>( );

        StringBuilder request = new StringBuilder( SQL_QUERY_SELECT_WRONG_ADRESSES );
        request.append( SQL_QUERY_WRONG_TOWN_SAINT_DENIS );
        request.append( SQL_QUERY_WRONG_TOWN_AUBERVILLIERS );
        request.append( SQL_QUERY_WRONG_TOWN_EPINAY_SUR_SAINE );
        request.append( SQL_QUERY_WRONG_TOWN_COURNEUVE );
        request.append( SQL_QUERY_WRONG_TOWN_ILE_SAINT_DENIS );
        request.append( SQL_QUERY_WRONG_TOWN_PIERREFITTE_SUR_SEINE );
        request.append( SQL_QUERY_WRONG_TOWN_SAINT_OUEN_SUR_SEINE );
        request.append( SQL_QUERY_WRONG_TOWN_STAINS );
        request.append( SQL_QUERY_WRONG_TOWN_VILLETANEUSE );
        request.append( SQL_QUERY_WHERE_ORDER_WRONG_TOWN_ );

        try ( DAOUtil daoUtil = new DAOUtil( request.toString( ) ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                Adresse adresse = new Adresse( );
                int nIndex = 1;
                adresse.setId( daoUtil.getLong( nIndex++ ) );
                adresse.setAdresse( daoUtil.getString( nIndex++ ) );
                adresse.setLng( daoUtil.getDouble( nIndex++ ) );
                adresse.setLat( daoUtil.getDouble( nIndex ) );
                result.add( adresse );
            }

        }

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_WRONG_ADRESSES_STREET ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                Adresse adresse = new Adresse( );
                int nIndex = 1;
                adresse.setId( daoUtil.getLong( nIndex++ ) );
                adresse.setAdresse( daoUtil.getString( nIndex++ ) );
                adresse.setLng( daoUtil.getDouble( nIndex++ ) );
                adresse.setLat( daoUtil.getDouble( nIndex ) );
                if (result.stream( ).filter( ad1 -> ad1.getId( ).longValue( ) == adresse.getId( ).longValue( ) ).count( ) < 1 ) {
                    result.add( adresse );
                }
            }

        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixSyntaxeArrondissement( )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_FIX_SYNTAXE_ARRONDISSEMENT );
        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixVille( )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_FIX_VILLE );
        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixVirguleCP( )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_FIX_VIRGULE_CP );
        daoUtil.executeUpdate( );
        daoUtil.close( );
    }

    /**
     * Sets the coordonate lambert 93 to WSG 84.
     *
     * @param adresse
     *            the adresse
     * @return the adresse
     */
    /*
     * (non-Javadoc)
     *
     * @see
     * fr.paris.lutece.plugins.dansmarue.business.dao.IAdresseDAO#setCoordonateLambert93ToWSG84(fr.paris.lutece.plugins.dansmarue.business.entities.Adresse)
     */
    @Override
    public Adresse setCoordonateLambert93ToWSG84( Adresse adresse )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_COORDONATE_WSG84 );
        daoUtil.setLong( 1, adresse.getId( ) );
        daoUtil.executeQuery( );

        if ( daoUtil.next( ) )
        {
            int nIndex = 1;

            adresse.setLng( daoUtil.getDouble( nIndex++ ) );
            adresse.setLat( daoUtil.getDouble( nIndex ) );
        }

        daoUtil.close( );

        return adresse;
    }
}
