(ns silk.input.pipeline
  "TODO : rename ns to something better... (webapp ?)
   Generic inputs (payloads) within a transformation pipeline.")

(def pipe-data {:data []
                :page {:offset nil :limit nil :total nil}
                :search nil
                :uri nil})
