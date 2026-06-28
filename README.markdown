# JSphinx

Sample code for interacting with [Sphinx](http://sphinxsearch.com/) in Java.

## Overview

JSphinx wraps the Sphinx Java API to provide a simpler, object-oriented interface for full-text search. The main classes are:  
Use this code for whatever you like, no licence applies.

- **`SearchCommand`** — the query parameters (search phrase, pagination, sort, facets)
- **`SearchService`** — abstract service that executes queries against Sphinx
- **`SearchResultContainer`** — holds the results: matched IDs, weights, total count, timing, and facets
- **`SearchMatch`** — a single result with its document ID and relevance weight

## Setup

### Required libraries

- [SLF4J](https://www.slf4j.org) (runtime dependency for logging)  
[See here](https://www.slf4j.org/manual.html#swapping) for details on how to choose and configure an SLF4J logging library.

Create a `Properties` object with your Sphinx connection details and pass it to your `SearchService` subclass:

```java
Properties props = new Properties();
props.setProperty("sphinxHost", "localhost");
props.setProperty("sphinxPort", "9313");
props.setProperty("sphinxIndexCommand", "/usr/bin/indexer");
props.setProperty("sphinxConfigFile", "/etc/sphinx/sphinx.conf");

MySearchService service = new MySearchService(props);
```

`sphinxHost` and `sphinxPort` default to `localhost` and `9313` if omitted. `sphinxIndexCommand` and `sphinxConfigFile` are required and used when triggering a delta re-index.

## Implementing your SearchService

Subclass `SearchService` and implement three methods:

```java
public class ArticleSearchService extends SearchService<ArticleSearchCommand>
{
    public ArticleSearchService(Properties props)
    {
        super(props);
    }

    @Override
    protected Map<String, Integer> createFieldWeightings()
    {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("title", 10);
        weights.put("tags",  5);
        weights.put("body",  1);
        return weights;
    }

    @Override
    protected void addFilters(ArticleSearchCommand cmd, SphinxClient sphinx) throws SphinxException
    {
        if (cmd.getCategoryId() != null)
        {
            sphinx.SetFilter("category_id", new long[]{ cmd.getCategoryId() }, false);
        }
    }

    @Override
    protected String getDeltaIndexName()
    {
        return "articles_delta";
    }
}
```

## Basic search

```java
SearchCommand cmd = new SearchCommand("java sphinx");
cmd.setPerPage(20);
cmd.setOffset(0);
cmd.setIndexNames("articles articles_delta");

SearchResultContainer results = service.search(cmd);

System.out.println("Total results: " + results.getTotalResults());
System.out.println("Query time: " + results.getTimeInSeconds() + "s");

// Document IDs in relevance order — use these to load records from your database
List<Long> ids = results.getSearchIds();

// Alternatively, access ID + weight together
for (SearchMatch match : results.getMatches())
{
    System.out.println("id=" + match.getSearchId() + " weight=" + match.getWeight());
}
```

## Sorting

When a search phrase is present, results are sorted by relevance first, then by the field returned from `getSortField()`. Override both methods in your `SearchCommand` subclass:

```java
public class ArticleSearchCommand extends SearchCommand
{
    @Override
    public String getSortField()
    {
        return "published_at";
    }

    @Override
    public SortOrder getSortOrder()
    {
        return SortOrder.DESC;
    }
}
```

When no search phrase is set, results are sorted purely by `getSortField()` / `getSortOrder()`.

## Faceted search

Add facet fields to your command before searching. Each facet field must be a Sphinx attribute (integer or string) defined in your index. Results are sorted by count descending, capped at 100 values per facet.

```java
SearchCommand cmd = new SearchCommand("java sphinx");
cmd.addFacetField("category_id");
cmd.addFacetField("author_id");

SearchResultContainer results = service.search(cmd);

Map<String, List<FacetEntry>> facets = results.getFacets();

List<FacetEntry> categories = facets.get("category_id");
for (FacetEntry entry : categories)
{
    System.out.println("category " + entry.getValue() + ": " + entry.getCount() + " results");
}
```

Facet counts respect the same filters applied to the main query, so they always reflect the current search context.

## Pagination

```java
int page = 3;
int perPage = 20;

SearchCommand cmd = new SearchCommand("java sphinx");
cmd.setPerPage(perPage);
cmd.setOffset((page - 1) * perPage);

SearchResultContainer results = service.search(cmd);
int totalPages = (int) Math.ceil((double) results.getTotalResults() / perPage);
```

Sphinx caps results at 1000 matches total (`MAX_MATCHES`). Offsets beyond that are clamped automatically.

## Delta re-indexing

Trigger a delta index rebuild at any time:

```java
service.reIndexDelta();
```

This runs the `sphinxIndexCommand` with `--rotate` on the delta index. A `SphinxException` is thrown if the indexer exits with a non-zero status.
