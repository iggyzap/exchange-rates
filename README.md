exchange-rates
==============

Tryout of Play 1.2.5 and some statistics. Lies, big lies and you know...

Not much yet.

ECB exchange rates are there : http://www.ecb.int/stats/exchange/eurofxref/html/index.en.html

To run application execute play 1.2.5 from play-module with play run and point browser to localhost:8080/

Main points of interest are - FxController, CsvTabulator & ecb-*.xsd schema files (that were used to generate source code in models.iggy.zap.fx package.

PS. It uses https://github.com/mbostock/d3 to render exchange rates, utilizing this Multi-series chart as baseline
( http://bl.ocks.org/3884955 )

