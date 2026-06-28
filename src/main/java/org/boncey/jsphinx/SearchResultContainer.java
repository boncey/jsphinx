package org.boncey.jsphinx;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A wrapper around any search results.
 *
 * @author Darren Greaves
 * Copyright (c) 2010 Darren Greaves.
 */
public class SearchResultContainer
{

    /**
     * The total number of matches.
     */
    private final int _totalResults;

    /**
     * The time the search took in seconds.
     */
    private float _timeInSeconds;

    /**
     * The ids of the search results.
     */
    private final List<Long> _searchIds;

    /**
     * The search results combining ids and weights.
     * Use as an alternative to #getSearchIds if you want the weight for each result.
     */
    private final List<SearchMatch> _matches;

    /**
     * Facet counts keyed by attribute name, then by attribute value.
     */
    private final Map<String, List<FacetEntry>> _facets;

    /**
     * Legacy field constructor.
     *
     * @param searchIds
     * @param totalResults
     */
    public SearchResultContainer(List<Long> searchIds, int totalResults)
    {

        this(searchIds, totalResults, Collections.emptyList(), 0f, Collections.emptyMap());
    }

    /**
     * Field constructor.
     *
     * @param searchIds
     * @param totalResults
     * @param matches
     * @param timeInSeconds
     */
    public SearchResultContainer(List<Long> searchIds, int totalResults, List<SearchMatch> matches, float timeInSeconds)
    {

        this(searchIds, totalResults, matches, timeInSeconds, Collections.emptyMap());
    }

    /**
     * Field constructor with facets.
     *
     * @param searchIds
     * @param totalResults
     * @param matches
     * @param timeInSeconds
     * @param facets
     */
    public SearchResultContainer(List<Long> searchIds, int totalResults, List<SearchMatch> matches, float timeInSeconds,
            Map<String, List<FacetEntry>> facets)
    {

        _searchIds = searchIds;
        _totalResults = totalResults;
        _matches = matches;
        _timeInSeconds = timeInSeconds;
        _facets = facets;
    }

    /**
     * Get the searchIds.
     *
     * @return the searchIds.
     */
    public List<Long> getSearchIds()
    {

        return _searchIds;
    }

    /**
     * Get the totalResults.
     *
     * @return the totalResults.
     */
    public int getTotalResults()
    {

        return _totalResults;
    }

    public List<SearchMatch> getMatches()
    {
        return _matches;
    }

    public Map<String, List<FacetEntry>> getFacets()
    {
        return _facets;
    }

    public float getTimeInSeconds()
    {
        return _timeInSeconds;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("boxing")
    @Override
    public String toString()
    {

        return String.format("SearchResultContainer [_searchIds=%s, _totalResults=%s]", _searchIds, _totalResults);
    }
}
