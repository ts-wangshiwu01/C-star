# Default period is daily, resetting at 00:00 China time (UTC+8)

The default accounting period is one day, resetting at 00:00 China time (UTC+8). The period length itself is configurable per deployment, but the default configuration ships as daily.

We chose a daily reset over the more common monthly cadence because a short, repeating quota keeps recognition timely and prevents stockpiling, while still being predictable. The accepted cost is that daily cadence requires a smaller quota per period and a higher-frequency reset job; deployments that prefer a longer rhythm (weekly/monthly) can configure it.
