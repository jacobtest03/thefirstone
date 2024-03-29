import { NoMatchComponent } from "@misk/components"
import * as React from "react"
import { Route, Switch } from "react-router"
import { LoaderContainer } from "./containers"

const routes = (
  <div>
    <Switch>
      <Route path="/_admin" component={LoaderContainer}/>
    </Switch>
  </div>
)

export default routes