package org.boncey.jsphinx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sphx.api.SphinxClient;
import org.sphx.api.SphinxException;
import org.sphx.api.SphinxMatch;
import org.sphx.api.SphinxResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Service for searching Sphinx.
 *
 * @author Darren Greaves
 * Copyright (c) 2010 Darren Greaves.
 */
public abstract class SearchService<S extends SearchCommand>
{

    /**
     * The maximum matches Sphinx can return.
     */
    public static final int MAX_MATCHES = 1000;

    /**
     * The maximum number of distinct values returned per facet.
     */
    public static final int MAX_FACET_VALUES = 100;

    /**
     * Logger for log4j.
     */
    private static Logger _log = LoggerFactory.getLogger(SearchService.class);

    public enum SortOrder
    {
        ASC, DESC;
    }

    /**
     * Sphinx port.
     */
    private int _port = 9313;

    /**
     * Sphinx host;
     */
    private String _host = "localhost";

    /**
     * The command that does the Sphinx indexing.
     */
    private final String _indexCommand;

    /**
     * The filename of the Sphinx config.
     */
    private final String _configFile;

    /**
     * Default constructor.
     *
     * @param properties
     *
     */
    public SearchService(Properties properties)
    {

        String host = properties.getProperty("sphinxHost");
        if (host != null)
        {
            _host = host;
        }
        String port = properties.getProperty("sphinxPort");
        if (port != null)
        {
            _port = Integer.parseInt(port);
        }

        _indexCommand = properties.getProperty("sphinxIndexCommand");
        _configFile = properties.getProperty("sphinxConfigFile");

        if (_indexCommand == null || _configFile == null)
        {
            throw new IllegalArgumentException("One of 'sphinxIndexCommand' or 'sphinxConfigFile' not set, cannot continue");
        }
    }

    /**
     * Search for the given parameters.
     *
     * @param searchCommand
     * @return a List of ids.
     * @throws SphinxException
     */
    public SearchResultContainer search(S searchCommand) throws SphinxException
    {

        int limit = searchCommand.getPerPage();
        int offset = searchCommand.getOffset() > MAX_MATCHES ? MAX_MATCHES - limit : searchCommand.getOffset();
        String searchPhrase = searchCommand.getSearchPhrase();
        List<String> facetFields = searchCommand.getFacetFields();

        SphinxClient sphinx = new SphinxClient();
        sphinx.SetServer(_host, _port);
        sphinx.SetFieldWeights(createFieldWeightings());
        sphinx.SetMatchMode(SphinxClient.SPH_MATCH_ALL);

        if (searchCommand.isSortByRelevance())
        {
            sphinx.SetSortMode(SphinxClient.SPH_SORT_EXTENDED,
                    String.format("@relevance DESC, %s %s", searchCommand.getSortField(), searchCommand.getSortOrder()));
        }
        else
        {
            int sortMode = searchCommand.getSortOrder() == SortOrder.DESC ?
                    SphinxClient.SPH_SORT_ATTR_DESC : SphinxClient.SPH_SORT_ATTR_ASC;
            sphinx.SetSortMode(sortMode, searchCommand.getSortField());
        }

        addFilters(searchCommand, sphinx);

        sphinx.SetLimits(offset, limit, MAX_MATCHES);
        sphinx.AddQuery(searchPhrase, searchCommand.getIndexNames(), "");

        for (String facetField : facetFields)
        {
            sphinx.SetLimits(0, MAX_FACET_VALUES, MAX_MATCHES);
            sphinx.SetGroupBy(facetField, SphinxClient.SPH_GROUPBY_ATTR, "@count desc");
            sphinx.AddQuery(searchPhrase, searchCommand.getIndexNames(), "");
        }

        SphinxResult[] results = sphinx.RunQueries();
        if (results == null || results.length == 0)
        {
            throw new SphinxException("Sphinx Error: " + sphinx.GetLastError());
        }

        SphinxResult mainResult = results[0];
        if (mainResult == null || mainResult.getStatus() == SphinxClient.SEARCHD_ERROR)
        {
            throw new SphinxException("Sphinx Error: " + sphinx.GetLastError());
        }

        if (mainResult.warning != null && mainResult.warning.length() > 0)
        {
            _log.warn("WARNING: " + mainResult.warning + "\n");
        }

        if (_log.isDebugEnabled())
        {
            _log.debug("Query '" + searchPhrase + "' retrieved " + mainResult.total + " of " + mainResult.totalFound + " matches in " + mainResult.time + " sec.");
        }

        List<Long> searchIds = new ArrayList<>();
        List<SearchMatch> matches = new ArrayList<>();
        for (SphinxMatch info : mainResult.matches)
        {
            searchIds.add(info.docId);
            matches.add(SearchMatch.fromSphinxMatch(info));
        }

        Map<String, List<FacetEntry>> facets = new LinkedHashMap<>();
        for (int i = 0; i < facetFields.size(); i++)
        {
            facets.put(facetFields.get(i), extractFacetEntries(results[i + 1]));
        }

        return new SearchResultContainer(searchIds, mainResult.total, matches, mainResult.time, facets);
    }

    private List<FacetEntry> extractFacetEntries(SphinxResult result)
    {

        List<FacetEntry> entries = new ArrayList<>();
        if (result == null || result.matches == null || result.attrNames == null)
        {
            return entries;
        }

        int groupByIndex = -1;
        int countIndex = -1;
        for (int i = 0; i < result.attrNames.length; i++)
        {
            if ("@groupby".equals(result.attrNames[i]))
            {
                groupByIndex = i;
            }
            else if ("@count".equals(result.attrNames[i]))
            {
                countIndex = i;
            }
        }

        if (groupByIndex == -1 || countIndex == -1)
        {
            return entries;
        }

        for (SphinxMatch match : result.matches)
        {
            String value = String.valueOf(match.attrValues.get(groupByIndex));
            int count = ((Long) match.attrValues.get(countIndex)).intValue();
            entries.add(new FacetEntry(value, count));
        }

        return entries;
    }

    /**
     * Get the field to sort by.
     *
     * @see <a href="http://sphinxsearch.com/docs/current.html#sorting-modes">Sphinx docs</a>
     *
     * @return
     * @deprecated See {@link SearchCommand#getSortField}
     */
    @Deprecated
    protected String getSortField()
    {
        return "";
    }

    /**
     * Re-index the delta index.
     *
     * @throws IOException
     * @throws SphinxException
     */
    public void reIndexDelta() throws IOException, SphinxException
    {

        Process proc = new ProcessBuilder(_indexCommand, "--config", _configFile, "--rotate", getDeltaIndexName()).start();

        try
        {
            proc.waitFor();
        }
        catch (InterruptedException e)
        {
            _log.error("Failed waiting for Sphinx re-index to complete");
        }

        if (proc.exitValue() != 0)
        {
            _log.error("Unable to re-index delta index");
            StringBuilder errorOutput = getProcessOutput(proc.getInputStream());
            _log.error(errorOutput.toString());
            throw new SphinxException(errorOutput.toString());
        }
        else if (_log.isDebugEnabled())
        {
            _log.debug("Output from Sphinx delta re-indexing");
            _log.debug(getProcessOutput(proc.getInputStream()).toString());
        }
    }

    /**
     * Return the name of the delta index.
     *
     * @return
     */
    protected abstract String getDeltaIndexName();

    /**
     * Get the process output.
     *
     * @param stream the error or stdout stream.
     * @return
     * @throws IOException
     */
    private StringBuilder getProcessOutput(InputStream stream) throws IOException
    {

        BufferedReader in = new BufferedReader(new InputStreamReader(stream));

        String line = in.readLine();
        StringBuilder output = new StringBuilder();
        while (line != null)
        {
            output.append(line);
            output.append("\n");

            line = in.readLine();
        }

        return output;
    }

    /**
     * Create a Map of field weightings.
     *
     * @see <a href="http://sphinxsearch.com/docs/current.html#weighting">Sphinx docs</a>
     *
     * @return
     */
    abstract protected Map<String, Integer> createFieldWeightings();

    /**
     * Filter by various properties.
     *
     * @see <a href="http://sphinxsearch.com/docs/current.html#delta-updates">Sphinx docs</a>
     *
     * @param searchCommand
     * @param sphinx
     * @throws SphinxException
     */
    abstract protected void addFilters(S searchCommand, SphinxClient sphinx) throws SphinxException;

    protected int getPort()
    {

        return _port;
    }

    protected String getHost()
    {

        return _host;
    }

    protected String getIndexCommand()
    {

        return _indexCommand;
    }

    protected String getConfigFile()
    {

        return _configFile;
    }

}
