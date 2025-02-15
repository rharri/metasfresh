Feature: create or update Purchase Candidate
  As a user
  I want create a Purchase Candidate record

  Background:
    Given the existing user with login 'metasfresh' receives a random a API token for the existing role with name 'WebUI'

    Given the user adds a purchase candidate
      | ExternalLineId | ExternalHeaderId | orgCode | warehouse        | isManualPrice | isManualDiscount | product     | vendor.id | qty | qty.uom | OPT.price | OPT.currency | OPT.priceUom | OPT.discount | OPT.purchaseDatePromised | OPT.purchaseDateOrdered |
      | L1             | H1               | 001     | val-StdWarehouse | false         | false            | val-P002737 | val-G0002 | 3   | PCE     |           |              |              |              |                          |                         |

    And the user adds a purchase candidate price
      | value | OPT.currencyCode | OPT.priceUomCode |
      | 1.5   | EUR              | PCE              |

    And the user adds attribute set instances
      | attributeName | attributeCode | valueStr | valueNumber | valueDate |
      |               |               |          |             |           |
    And the user adds a purchase candidate enqueue request
      | ExternalHeaderId | ExternalLineId |
      | H1               | L1             |

  Scenario:  The purchase candidate request is set in context, enqueued and validated
    And the purchase candidate request is set in context
    When the metasfresh REST-API endpoint path 'api/v2-pre/order/purchase/createCandidates' receives a 'POST' request with the payload from context and responds with '200' status code
    Then verify if data is persisted correctly for purchase candidate

    Given the purchase candidate enqueue-status request is set in context
    Then the metasfresh REST-API endpoint path 'api/v2-pre/order/purchase/enqueueForOrdering' receives a 'POST' request with the payload from context and responds with '202' status code

    Given the purchase candidate enqueue-status request is set in context
    When the metasfresh REST-API endpoint path 'api/v2-pre/order/purchase/status' receives a 'PUT' request with the payload from context and responds with '200' status code
    Then verify purchase candidate status