package org.boncey.jsphinx;

import org.apache.log4j.Logger;
import org.sphx.api.SphinxClient;
import org.sphx.api.SphinxException;
import org.sphx.api.SphinxMatch;
import org.sphx.api.SphinxResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
     * Logger for log4j.
     */
    private static Logger _log = Logger.getLogger(SearchService.class);

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

        int totalFound = 0;
        int limit = searchCommand.getPerPage();
        int offset = searchCommand.getOffset() > MAX_MATCHES ? MAX_MATCHES - limit : searchCommand.getOffset();
        String searchPhrase = searchCommand.getSearchPhrase();

        List<Long> searchIds = new ArrayList<Long>();
        int mode = SphinxClient.SPH_MATCH_ALL;

        SphinxClient sphinx = new SphinxClient();

        sphinx.SetServer(_host, _port);
        Map<String, Integer> fieldWeights = createFieldWeightings();
        sphinx.SetFieldWeights(fieldWeights);
        sphinx.SetMatchMode(mode);
        sphinx.SetLimits(offset, limit, MAX_MATCHES);

        if (searchCommand.isSortByRelevance())
        {
            // Sort by relevance then specified sort field
            sphinx.SetSortMode(SphinxClient.SPH_SORT_EXTENDED,
                    String.format("@relevance DESC, %s %s", searchCommand.getSortField(), searchCommand.getSortOrder()));
        }
        else
        {
            int sortMode = SphinxClient.SPH_SORT_ATTR_ASC;
            if (searchCommand.getSortOrder() == SortOrder.DESC)
            {
                sortMode = SphinxClient.SPH_SORT_ATTR_DESC;
            }
            // Sort purely by specified sort field
            sphinx.SetSortMode(sortMode, searchCommand.getSortField());
        }

        addFilters(searchCommand, sphinx);

        SphinxResult res = sphinx.Query(searchPhrase, searchCommand.getIndexNames());
        if (res == null)
        {
            throw new SphinxException("Sphinx Error: " + sphinx.GetLastError());
        }

        if (sphinx.GetLastWarning() != null && sphinx.GetLastWarning().length() > 0)
        {
            _log.warn("WARNING: " + sphinx.GetLastWarning() + "\n");
        }

        totalFound = res.total;
        if (_log.isDebugEnabled())
        {
            _log.debug(
                    "Query '" + searchCommand.getSearchPhrase() + "' retrieved " + res.total + " of " + res.totalFound + " matches in " + res.time + " sec.");
        }

        for (SphinxMatch info : res.matches)
        {
            searchIds.add(new Long(info.docId));
        }

        SearchResultContainer results = new SearchResultContainer(searchIds, totalFound);

        return results;
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
            _log.error(errorOutput);
            throw new SphinxException(errorOutput.toString());
        }
        else if (_log.isDebugEnabled())
        {
            _log.debug("Output from Sphinx delta re-indexing");
            _log.debug(getProcessOutput(proc.getInputStream()));
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
