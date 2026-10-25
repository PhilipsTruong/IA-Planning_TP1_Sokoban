(define (domain sokoban)
  (:requirements :strips :typing)
  (:types location direction)

  (:predicates
    (player-at ?l - location)
    (box-at ?l - location)
    (clear ?l - location)
    (adjacent ?from - location ?to - location ?d - direction)
    (can-push ?from - location ?bloc - location ?to - location ?d - direction)
  )

  (:action move
    :parameters (?from - location ?to - location ?d - direction)
    :precondition (and
      (player-at ?from)
      (adjacent ?from ?to ?d)
      (clear ?to)
    )
    :effect (and
      (player-at ?to)
      (not (player-at ?from))
    )
  )

  (:action push
    :parameters (?from - location ?bloc - location ?to - location ?d - direction)
    :precondition (and
      (player-at ?from)
      (box-at ?bloc)
      (clear ?to)
      (can-push ?from ?bloc ?to ?d)
    )
    :effect (and
      (player-at ?bloc)
      (not (player-at ?from))
      (box-at ?to)
      (not (box-at ?bloc))
      (clear ?bloc)
      (not (clear ?to))
    )
  )
)