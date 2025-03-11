# RWB Inactive Users Custom Query Business Logic
================================================

This document summarizes the business logic behind **"RWB Inactive Users"** PostgreSQL query that identifies specific users based on their group membership, catchment areas, and work order status.

## Overview

------------

The query involves several steps to filter users who meet certain criteria related to their group membership, catchment areas, and recent activity.

## Steps Involved

------------------

### 1. **Primary Users Identification**

--------------------------------------

- **Purpose**: Identify users who belong to the 'Primary Users' group and are not disabled in Cognito.
- **Conditions**:
  - Users must be part of the 'Primary Users' group.
  - Users must not be disabled in Cognito (`disabled_in_cognito = false`).
  - Users must either have no `last_activated_date_time` or it must be older than 3 days (`last_activated_date_time is null OR last_activated_date_time < now() - INTERVAL '3 DAYS'`).
  - Users must not be voided (`is_voided = false`).


### 2. **Work Orders Identification**

--------------------------------------

- **Purpose**: Identify work orders that are not voided and belong to a specific organization.
- **Conditions**:
  - Work orders must not be voided (`is_voided = false`).
  - Work orders must be of type 'Work Order' for a specific organization (`subject_type_id` matches the 'Work Order' type for the organization identified by `:org_db_user`).


### 3. **Closed Work Orders Identification**

--------------------------------------------

- **Purpose**: Identify work orders that have been closed.
- **Conditions**:
  - Work orders must have an encounter of type 'Work order endline' for the same organization.
  - The encounter must not be voided (`is_voided is null or is_voided = false`).
  - There must be exactly one such encounter per work order (`having count(e.id) = 1`).


### 4. **Catchments Without Work Orders or At Least One Open Work Order**

-------------------------------------------------------------------

- **Purpose**: Identify catchment areas that either have no work orders or have more open work orders than closed ones.
- **Conditions**:
  - Catchments must not be voided (`is_voided = false`).
  - Either there are no work orders (`count(wo.wo_id) = null`), or there are more work orders than closed work orders (`count(wo.wo_id) > count(cwo.wo_id)`).


### 5. **Active User IDs**

-------------------------

- **Purpose**: Identify user IDs that have been active recently (created or modified entities after a certain cutoff date).
- **Conditions**:
  - For individuals and encounters, if the creation or modification date is after the specified cutoff date, the created_by_id or last_modified_by_id is considered an active user ID.


### 6. **Final Selection**

-------------------------

- **Purpose**: Select primary users who are in catchments without work orders or at least one open work order and have not been active recently.
- **Conditions**:
  - Users must be primary users in catchments identified in step 4.
  - Users must not have been active recently (their IDs must not be in the list of active user IDs).


## Summary

----------

This query identifies primary users in catchments without work order or atleast one open wor-order, who have not been recently active in the system. It ensures that these users meet all the specified criteria related to their group membership, catchment areas, and recent activity.

## IMPORTANT Additional Details

- The CutOffDate for determining in-activity is 3 Days from current date.
- Also, if the user has been nudged in the last 3 days, we do not nudge him again.
