# Repair Guide — release readiness

**Status:** staged on `feature/vehicle-registration-context`; not merged, deployed, or sent to testers.

## Intended user flow

1. The user selects up to three photos and enters vehicle make, model, year, and engine details when relevant.
2. Object Detective identifies the likely item and explains which visible clues support the result.
3. The app shows what the item does and, when reliable guidance is available, a repair guide with tools, steps, difficulty, safety checks, and links to its sources.
4. Automotive fitment stays marked as unconfirmed until an exact part number or trusted fitment source confirms it.
5. The user can continue to relevant buying or selling options.

## Ready in this branch

- Optional manual vehicle details are collected by the investigation screen and passed to the existing investigation function as context.
- The registration text field is for the user's reference only. It is not sent for lookup or analysis and is not saved in local investigation history.
- The investigation prompt treats manually entered vehicle details as unverified context and prohibits unsupported fitment claims.
- The privacy policy describes this data flow.
- [Interactive screen prototype](repair-guide-prototype.html) shows sample automotive and home-DIY paths, fitment caveats, tools, steps, and safety messages. Its sample results are not live advice.

## Work still needed before a live Repair Guide can be released

- Select repair and parts information sources whose terms permit this use. Confirm the source coverage for each supported category.
- Build a server-side source retrieval step. The guide should show source names and links beside the guidance.
- Add a structured guide response to the app: likely task, supporting evidence, tools, numbered steps, difficulty, safety level, fitment status, and source links.
- Keep exact fitting claims disabled when the app cannot verify the component, vehicle variant, or procedure.
- Define categories that the app will decline to guide in the first release, including safety-critical work that needs a qualified person.
- Update the privacy policy and Play Data safety information if the eventual service provider or data flow differs from this staged version.

## Deferred

- Automatic registration lookup is intentionally not implemented. A registration plate alone is not used to identify a vehicle in this version. Automatic lookup can be evaluated later after the user-facing value, API access, privacy terms, and cost are clear.
- This prototype does not analyse a real photo, verify a repair procedure, confirm part fitment, or connect to retailer catalogues.

## Release sequence

1. Review the prototype and confirm the first supported repair categories.
2. Confirm permitted, trustworthy source data and a way to retrieve it securely.
3. Implement the live guide response and show evidence, fitment status, and safety limits in the app.
4. Review the completed flow and build it on this feature branch.
5. After the current closed-test period, decide whether to merge and publish a new testing build.

Keep this branch separate from the current Play testing release until the live guide and its source checks are ready.
