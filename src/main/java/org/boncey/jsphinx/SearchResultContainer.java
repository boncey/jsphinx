package org.boncey.jsphinx;

import java.util.Collections;
import java.util.List;

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
     * Legacy field constructor.
     *
     * @param searchIds
     * @param totalResults
     */
    public SearchResultContainer(List<Long> searchIds, int totalResults)
    {

        this(searchIds, totalResults, Collections.emptyList(), 0f);
    }

    /**
     * Field constructor.
     *
     * @param searchIds
     * @param totalResults
     * @param matches
     */
    public SearchResultContainer(List<Long> searchIds, int totalResults, List<SearchMatch> matches, float timeInSeconds)
    {

        _searchIds = searchIds;
        _totalResults = totalResults;
        _matches = matches;
        _timeInSeconds = timeInSeconds;
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
