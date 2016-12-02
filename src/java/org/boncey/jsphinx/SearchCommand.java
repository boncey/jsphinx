package org.boncey.jsphinx;

import org.apache.log4j.Logger;
import org.boncey.jsphinx.SearchService.SortOrder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Command object for searching.
 *
 * @author Darren Greaves
 * Copyright (c) 2010 Darren Greaves.
 */
public class SearchCommand
{

    /**
     * Results to show per page.
     */
    private static final int PER_PAGE = 100;

    /**
     * Logger for log4j.
     */
    private static Logger _log = Logger.getLogger(SearchCommand.class);

    /**
     * The text to search for.
     */
    private String _searchPhrase;

    /**
     * The results to offset by.
     */
    private int _offset;

    /**
     * How many results to return.
     */
    private int _perPage;

    /**
     * The names of any indexes to search (default is '*' which searches all indexes).
     *
     * Note: Use spaces to separate multiple indexes.
     */
    private String _indexNames;

    /**
     * Default constructor.
     */
    public SearchCommand()
    {
        _offset = 0;
        _perPage = PER_PAGE;
        _indexNames = "*";
    }

    /**
     *
     * @param searchPhrase
     */
    public SearchCommand(String searchPhrase)
    {

        this();
        _searchPhrase = searchPhrase;
    }

    /**
     * Get the searchPhrase.
     *
     * @return the searchPhrase.
     */
    public String getSearchPhrase()
    {

        return _searchPhrase;
    }

    /**
     * Set the searchPhrase.
     *
     * @param searchPhrase the searchPhrase to set.
     */
    public void setSearchPhrase(String searchPhrase)
    {

        _searchPhrase = searchPhrase;
    }

    /**
     * Get the offset.
     *
     * @return the offset.
     */
    public int getOffset()
    {

        return _offset;
    }

    /**
     * Set the offset.
     *
     * @param offset the offset to set.
     */
    public void setOffset(int offset)
    {

        _offset = offset;
    }

    /**
     * Get the perPage.
     *
     * @return the perPage.
     */
    public int getPerPage()
    {

        return _perPage;
    }

    /**
     * Set the perPage.
     *
     * @param perPage the perPage to set.
     */
    public void setPerPage(int perPage)
    {

        _perPage = perPage;
    }

    public String getIndexNames()
    {
        return _indexNames;
    }

    public void setIndexNames(String indexNames)
    {
        _indexNames = indexNames;
    }

    /**
     * Is this a search by relevance or search by the specified sort field?
     *
     * @return
     *
     * @see SearchCommand#getSortField()
     */
    public boolean isSortByRelevance()
    {
        return _searchPhrase != null && !_searchPhrase.isEmpty();
    }

    /**
     * Get the field to sort by. Override with your sort field.
     *
     * @see <a href="http://sphinxsearch.com/docs/current.html#sorting-modes">Sphinx docs</a>
     *
     * @return
     */
    public String getSortField()
    {
        return "";
    }

    /**
     * Get the sort ordering. Override with your sort ordering.
     *
     * @see <a href="http://sphinxsearch.com/docs/current.html#sorting-modes">Sphinx docs</a>
     *
     * @return
     *
     */
    public SortOrder getSortOrder()
    {
        return SortOrder.DESC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("SearchCommand: %s", _searchPhrase);
    }

    /**
     * Get the query String for this search.
     *
     * @return the query String.
     */
    public String getQueryString()
    {

        StringBuilder builder = new StringBuilder();

        List<String> fields = new ArrayList<String>();
        if (_searchPhrase != null)
        {
            try
            {
                fields.add("search=" + URLEncoder.encode(_searchPhrase, "UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                _log.error("Unable to encode " + _searchPhrase);
            }
        }

        for (String field : fields)
        {
            builder.append((builder.length() == 0) ? "" : "&");
            builder.append(field);
        }

        return builder.toString();
    }

}
