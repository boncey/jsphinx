package org.boncey.jsphinx;

public class FacetEntry
{
    private final String _value;

    private final int _count;

    public FacetEntry(String value, int count)
    {
        _value = value;
        _count = count;
    }

    public String getValue()
    {
        return _value;
    }

    public int getCount()
    {
        return _count;
    }

    @Override
    public String toString()
    {
        return "FacetEntry{_value='" + _value + "', _count=" + _count + '}';
    }
}