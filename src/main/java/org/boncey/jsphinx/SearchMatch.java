package org.boncey.jsphinx;

import org.sphx.api.SphinxMatch;

public class SearchMatch
{
    private final Long _searchId;

    private final int _weight;

    public SearchMatch(Long searchId, int weight)
    {
        _searchId = searchId;
        _weight = weight;
    }

    public static SearchMatch fromSphinxMatch(SphinxMatch match)
    {
        return new SearchMatch(match.docId, match.weight);
    }

    public Long getSearchId()
    {
        return _searchId;
    }

    public int getWeight()
    {
        return _weight;
    }

    @Override
    public String toString()
    {
        return "SearchMatch{" +
                "_searchId=" + _searchId +
                ", _weight=" + _weight +
                '}';
    }
}
